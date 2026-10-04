package com.aislam.rag.service;

import com.aislam.rag.client.NativePushClient;
import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.dto.DailyContentResponse;
import com.aislam.rag.entity.AppPushDeliveryEntity;
import com.aislam.rag.entity.DevicePushTokenEntity;
import com.aislam.rag.repository.AppPushDeliveryRepository;
import com.aislam.rag.repository.DevicePushTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Günlük içerik + yarışma bildirimleri — doğrudan FCM/APNs.
 */
@Service
public class AppNotificationPushSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(AppNotificationPushSchedulerService.class);

    private static final List<DailySpec> DAILY = List.of(
            new DailySpec("ayet", "Günün Ayeti", true),
            new DailySpec("dua", "Günün Duası", true),
            new DailySpec("hadis", "Günün Hadisi", true),
            new DailySpec("hutbe", "Cuma Hutbesi", false),
            new DailySpec("asma", "Esmaül Hüsna", false)
    );

    private static final int COMPETITION_HOUR = 20;
    private static final int COMPETITION_MINUTE = 0;

    private final PushProperties pushProperties;
    private final DailyProperties dailyProperties;
    private final DevicePushTokenRepository tokenRepository;
    private final AppPushDeliveryRepository deliveryRepository;
    private final DailyContentService dailyContentService;
    private final NativePushClient nativePushClient;

    public AppNotificationPushSchedulerService(
            PushProperties pushProperties,
            DailyProperties dailyProperties,
            DevicePushTokenRepository tokenRepository,
            AppPushDeliveryRepository deliveryRepository,
            DailyContentService dailyContentService,
            NativePushClient nativePushClient
    ) {
        this.pushProperties = pushProperties;
        this.dailyProperties = dailyProperties;
        this.tokenRepository = tokenRepository;
        this.deliveryRepository = deliveryRepository;
        this.dailyContentService = dailyContentService;
        this.nativePushClient = nativePushClient;
    }

    @Scheduled(fixedDelayString = "${app.push.poll-interval-ms:30000}")
    public void tick() {
        if (!pushProperties.enabled()) {
            return;
        }
        try {
            dispatch();
        } catch (Exception ex) {
            log.warn("App notification push tick failed: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void dispatch() {
        List<DevicePushTokenEntity> devices = tokenRepository.findByEnabledTrue();
        if (devices.isEmpty()) {
            return;
        }

        ZoneId zoneId = dailyProperties.zoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime windowStart = now.minusSeconds(pushProperties.lookbackSeconds());
        ZonedDateTime windowEnd = now.plusSeconds(pushProperties.lookaheadSeconds());
        LocalDate today = now.toLocalDate();
        int weekday = today.getDayOfWeek().getValue() % 7;
        String dateKey = today.toString();

        DailyContentResponse dailyContent = dailyContentService.getToday();

        List<NativePushClient.PushMessage> batch = new ArrayList<>();
        List<Pending> pending = new ArrayList<>();

        for (DevicePushTokenEntity device : devices) {
            collectDaily(device, dailyContent, dateKey, weekday, zoneId, windowStart, windowEnd, batch, pending);
            collectCompetition(device, dateKey, weekday, zoneId, windowStart, windowEnd, batch, pending);
        }

        if (batch.isEmpty()) {
            return;
        }

        List<NativePushClient.PushTicket> tickets = nativePushClient.send(batch);
        int sent = 0;
        for (int i = 0; i < pending.size(); i += 1) {
            Pending item = pending.get(i);
            NativePushClient.PushTicket ticket = i < tickets.size() ? tickets.get(i) : null;
            if (ticket != null && ticket.ok()) {
                markDelivered(item);
                sent += 1;
            } else if (ticket != null) {
                handlePushError(item.deviceId(), ticket);
            }
        }

        if (sent > 0) {
            log.info("App notification push sent count={}", sent);
        }
    }

    private void collectDaily(
            DevicePushTokenEntity device,
            DailyContentResponse content,
            String dateKey,
            int weekday,
            ZoneId zoneId,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            List<NativePushClient.PushMessage> batch,
            List<Pending> pending
    ) {
        Map<String, Object> prefs = device.getDailyPrefs();
        if (prefs == null || prefs.isEmpty()) {
            return;
        }

        for (DailySpec spec : DAILY) {
            Object raw = prefs.get(spec.id());
            if (!(raw instanceof Map<?, ?> pref)) {
                continue;
            }
            if (!asBoolean(pref.get("enabled"), false)) {
                continue;
            }
            if (!isWeekdayEnabled(pref.get("days"), weekday)) {
                continue;
            }

            int hour = asInt(pref.get("hour"), 10);
            int minute = asInt(pref.get("minute"), 0);
            ZonedDateTime fireAt = ZonedDateTime.of(
                    LocalDateTime.of(LocalDate.parse(dateKey), LocalTime.of(hour, minute)),
                    zoneId
            );
            if (fireAt.isBefore(windowStart) || fireAt.isAfter(windowEnd)) {
                continue;
            }
            if (deliveryRepository.existsByDeviceIdAndEventDateAndChannelAndItemKeyAndKind(
                    device.getDeviceId(), dateKey, "daily", spec.id(), "fire"
            )) {
                continue;
            }

            int melodyIndex = asInt(pref.get("melodyIndex"), 0);
            String body = bodyForDaily(spec, content);
            Map<String, String> data = new HashMap<>();
            data.put("type", "daily-content");
            data.put("dailyKind", spec.id());
            data.put("openHome", spec.openHome() ? "true" : "false");

            batch.add(new NativePushClient.PushMessage(
                    device.getPushToken(),
                    device.getPlatform(),
                    spec.label(),
                    body,
                    data,
                    "notif-sound-v3-" + Math.max(0, melodyIndex)
            ));
            pending.add(new Pending(device.getDeviceId(), dateKey, "daily", spec.id(), "fire"));
        }
    }

    private void collectCompetition(
            DevicePushTokenEntity device,
            String dateKey,
            int weekday,
            ZoneId zoneId,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            List<NativePushClient.PushMessage> batch,
            List<Pending> pending
    ) {
        Map<String, Object> prefs = device.getCompetitionPrefs();
        if (prefs == null || prefs.isEmpty()) {
            return;
        }
        Object raw = prefs.get("daily");
        if (!(raw instanceof Map<?, ?> pref)) {
            return;
        }
        if (!isWeekdayEnabled(pref.get("days"), weekday)) {
            return;
        }

        ZonedDateTime atTime = ZonedDateTime.of(
                LocalDateTime.of(LocalDate.parse(dateKey), LocalTime.of(COMPETITION_HOUR, COMPETITION_MINUTE)),
                zoneId
        );

        maybeQueueCompetition(
                device, pref, "atTime", dateKey, atTime, windowStart, windowEnd,
                false, 0, batch, pending
        );

        Object beforeRaw = pref.get("before");
        if (beforeRaw instanceof Map<?, ?> beforeMap) {
            int minutesBefore = asInt(beforeMap.get("minutesBefore"), 15);
            maybeQueueCompetition(
                    device, pref, "before", dateKey, atTime.minusMinutes(minutesBefore),
                    windowStart, windowEnd, true, minutesBefore, batch, pending
            );
        }
    }

    private void maybeQueueCompetition(
            DevicePushTokenEntity device,
            Map<?, ?> pref,
            String kind,
            String dateKey,
            ZonedDateTime fireAt,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            boolean isBefore,
            int minutesBefore,
            List<NativePushClient.PushMessage> batch,
            List<Pending> pending
    ) {
        Object kindRaw = pref.get(kind);
        if (!(kindRaw instanceof Map<?, ?> kindMap)) {
            return;
        }
        if (!asBoolean(kindMap.get("enabled"), false)) {
            return;
        }
        if (fireAt.isBefore(windowStart) || fireAt.isAfter(windowEnd)) {
            return;
        }
        if (deliveryRepository.existsByDeviceIdAndEventDateAndChannelAndItemKeyAndKind(
                device.getDeviceId(), dateKey, "competition", "daily", kind
        )) {
            return;
        }

        int melodyIndex = asInt(kindMap.get("melodyIndex"), 0);
        String title = isBefore ? "Günlük Yarışma başlamak üzere" : "Günlük Yarışma başladı!";
        String body = isBefore
                ? "Günlük Yarışma " + minutesBefore + " dakika içinde başlıyor. Hazır ol!"
                : "Günlük Yarışma başladı. Hemen katıl!";

        Map<String, String> data = new HashMap<>();
        data.put("type", "competition");
        data.put("competitionKind", "daily");
        data.put("kind", kind);
        data.put("openHome", "true");

        batch.add(new NativePushClient.PushMessage(
                device.getPushToken(),
                device.getPlatform(),
                title,
                body,
                data,
                "notif-sound-v3-" + Math.max(0, melodyIndex)
        ));
        pending.add(new Pending(device.getDeviceId(), dateKey, "competition", "daily", kind));
    }

    private static String bodyForDaily(DailySpec spec, DailyContentResponse content) {
        return switch (spec.id()) {
            case "ayet" -> snippet(content.ayet() != null ? content.ayet().text() : null,
                    "Bugünün ayetini okumak için dokunun...");
            case "dua" -> snippet(content.dua() != null ? content.dua().text() : null,
                    "Bugünün duasını okumak için dokunun...");
            case "hadis" -> snippet(content.hadis() != null ? content.hadis().text() : null,
                    "Bugünün hadisini okumak için dokunun...");
            case "hutbe" -> "Bu haftanın Cuma hutbesini okumak için dokunun...";
            case "asma" -> "Bugünün isminin anlamını biliyor musunuz?";
            default -> "e-İslam";
        };
    }

    private static String snippet(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        String[] words = cleaned.split(" ");
        if (words.length <= 8) {
            return cleaned.endsWith("...") ? cleaned : cleaned + "...";
        }
        return String.join(" ", java.util.Arrays.copyOf(words, 8)) + "...";
    }

    private void markDelivered(Pending pending) {
        try {
            deliveryRepository.save(new AppPushDeliveryEntity(
                    UUID.randomUUID(),
                    pending.deviceId(),
                    pending.eventDate(),
                    pending.channel(),
                    pending.itemKey(),
                    pending.kind()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // duplicate
        }
    }

    private void handlePushError(String deviceId, NativePushClient.PushTicket ticket) {
        String code = ticket.errorCode() != null ? ticket.errorCode() : "";
        tokenRepository.findByDeviceId(deviceId).ifPresent(entity -> {
            entity.setLastError(code + (ticket.message() != null ? (": " + ticket.message()) : ""));
            String normalized = code.toUpperCase();
            if (normalized.contains("UNREGISTERED")
                    || normalized.contains("INVALID")
                    || normalized.contains("BADDEVICETOKEN")) {
                entity.setEnabled(false);
            }
            tokenRepository.save(entity);
        });
    }

    private static boolean isWeekdayEnabled(Object daysRaw, int sundayBasedWeekday) {
        if (!(daysRaw instanceof List<?> days) || days.size() < 7) {
            return true;
        }
        return asBoolean(days.get(sundayBasedWeekday), true);
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return fallback;
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private record DailySpec(String id, String label, boolean openHome) {
    }

    private record Pending(String deviceId, String eventDate, String channel, String itemKey, String kind) {
    }
}
