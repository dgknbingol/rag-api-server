package com.aislam.rag.service;

import com.aislam.rag.client.NativePushClient;
import com.aislam.rag.config.PrayerTimesProperties;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.dto.MonthlyPrayerTimesResponse;
import com.aislam.rag.dto.PrayerDayDto;
import com.aislam.rag.entity.DevicePushTokenEntity;
import com.aislam.rag.entity.PrayerPushDeliveryEntity;
import com.aislam.rag.repository.DevicePushTokenRepository;
import com.aislam.rag.repository.PrayerPushDeliveryRepository;
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
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PrayerPushSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PrayerPushSchedulerService.class);
    private static final long EMPTY_DEVICES_LOG_INTERVAL_MS = 10 * 60 * 1000L;
    private final AtomicLong lastEmptyDevicesLogAt = new AtomicLong(0);

    private static final List<PrayerSpec> PRAYERS = List.of(
            new PrayerSpec("imsak", "İmsak", "imsak"),
            new PrayerSpec("gunes", "Güneş", "gunes"),
            new PrayerSpec("ogle", "Öğle", "ogle"),
            new PrayerSpec("ikindi", "İkindi", "ikindi"),
            new PrayerSpec("aksam", "Akşam", "aksam"),
            new PrayerSpec("yatsi", "Yatsı", "yatsi")
    );

    private final PushProperties pushProperties;
    private final PrayerTimesProperties prayerTimesProperties;
    private final DevicePushTokenRepository tokenRepository;
    private final PrayerPushDeliveryRepository deliveryRepository;
    private final PrayerTimesService prayerTimesService;
    private final NativePushClient nativePushClient;

    public PrayerPushSchedulerService(
            PushProperties pushProperties,
            PrayerTimesProperties prayerTimesProperties,
            DevicePushTokenRepository tokenRepository,
            PrayerPushDeliveryRepository deliveryRepository,
            PrayerTimesService prayerTimesService,
            NativePushClient nativePushClient
    ) {
        this.pushProperties = pushProperties;
        this.prayerTimesProperties = prayerTimesProperties;
        this.tokenRepository = tokenRepository;
        this.deliveryRepository = deliveryRepository;
        this.prayerTimesService = prayerTimesService;
        this.nativePushClient = nativePushClient;
    }

    @Scheduled(fixedDelayString = "${app.push.poll-interval-ms:30000}")
    public void tick() {
        if (!pushProperties.enabled()) {
            return;
        }
        try {
            dispatchDueNotifications();
        } catch (Exception ex) {
            log.warn("Prayer push tick failed: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void dispatchDueNotifications() {
        List<DevicePushTokenEntity> devices = tokenRepository.findByEnabledTrue();
        if (devices.isEmpty()) {
            long nowMs = System.currentTimeMillis();
            long prev = lastEmptyDevicesLogAt.get();
            if (nowMs - prev >= EMPTY_DEVICES_LOG_INTERVAL_MS
                    && lastEmptyDevicesLogAt.compareAndSet(prev, nowMs)) {
                log.info("Prayer push: enabled device yok (register bekleniyor)");
            }
            return;
        }

        ZoneId zoneId = prayerTimesProperties.zoneId();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime windowStart = now.minusSeconds(pushProperties.lookbackSeconds());
        ZonedDateTime windowEnd = now.plusSeconds(pushProperties.lookaheadSeconds());

        Map<String, MonthlyPrayerTimesResponse> timesByCoordKey = new HashMap<>();
        List<NativePushClient.PushMessage> batch = new ArrayList<>();
        List<PendingDelivery> pending = new ArrayList<>();

        for (DevicePushTokenEntity device : devices) {
            String coordKey = coordKey(device.getLatitude(), device.getLongitude());
            MonthlyPrayerTimesResponse monthly = timesByCoordKey.computeIfAbsent(
                    coordKey,
                    ignored -> prayerTimesService.getMonthly(
                            device.getLatitude(),
                            device.getLongitude(),
                            now.getYear(),
                            now.getMonthValue()
                    )
            );

            collectDueEvents(device, monthly, zoneId, windowStart, windowEnd, batch, pending);

            // Ay değişiminde yarın için bir sonraki ay gerekebilir (ayın son günü).
            if (now.getDayOfMonth() >= 28) {
                LocalDate tomorrow = now.toLocalDate().plusDays(1);
                if (tomorrow.getMonthValue() != now.getMonthValue()) {
                    MonthlyPrayerTimesResponse nextMonth = prayerTimesService.getMonthly(
                            device.getLatitude(),
                            device.getLongitude(),
                            tomorrow.getYear(),
                            tomorrow.getMonthValue()
                    );
                    collectDueEvents(device, nextMonth, zoneId, windowStart, windowEnd, batch, pending);
                }
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        List<NativePushClient.PushTicket> tickets = nativePushClient.send(batch);
        int sent = 0;
        for (int i = 0; i < pending.size(); i += 1) {
            PendingDelivery delivery = pending.get(i);
            NativePushClient.PushTicket ticket = i < tickets.size() ? tickets.get(i) : null;
            if (ticket != null && ticket.ok()) {
                markDelivered(delivery);
                sent += 1;
            } else if (ticket != null) {
                handlePushError(delivery.deviceId(), ticket);
            }
        }

        if (sent > 0) {
            log.info("Prayer push sent count={} devices={}", sent, devices.size());
        }
    }

    private void collectDueEvents(
            DevicePushTokenEntity device,
            MonthlyPrayerTimesResponse monthly,
            ZoneId zoneId,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            List<NativePushClient.PushMessage> batch,
            List<PendingDelivery> pending
    ) {
        Map<String, Object> prefs = device.getPrayerPrefs();
        if (prefs == null || prefs.isEmpty()) {
            return;
        }

        for (PrayerDayDto day : monthly.days()) {
            LocalDate date = LocalDate.parse(day.date());
            int weekday = date.getDayOfWeek().getValue() % 7; // Mon=1..Sun=7 → Sun=0

            for (PrayerSpec prayer : PRAYERS) {
                Object rawPref = prefs.get(prayer.id());
                if (!(rawPref instanceof Map<?, ?> prefMap)) {
                    continue;
                }

                String timeText = timeFor(day, prayer.timeKey());
                LocalTime time = parseTime(timeText);
                if (time == null) {
                    continue;
                }

                ZonedDateTime atTime = ZonedDateTime.of(LocalDateTime.of(date, time), zoneId);

                maybeQueue(
                        device,
                        prefMap,
                        "atTime",
                        prayer,
                        day.date(),
                        weekday,
                        atTime,
                        windowStart,
                        windowEnd,
                        false,
                        0,
                        batch,
                        pending
                );

                Object beforeRaw = prefMap.get("before");
                if (beforeRaw instanceof Map<?, ?> beforeMap) {
                    int minutesBefore = asInt(beforeMap.get("minutesBefore"), 15);
                    ZonedDateTime beforeTime = atTime.minusMinutes(minutesBefore);
                    maybeQueue(
                            device,
                            prefMap,
                            "before",
                            prayer,
                            day.date(),
                            weekday,
                            beforeTime,
                            windowStart,
                            windowEnd,
                            true,
                            minutesBefore,
                            batch,
                            pending
                    );
                }
            }
        }
    }

    private void maybeQueue(
            DevicePushTokenEntity device,
            Map<?, ?> prefMap,
            String kind,
            PrayerSpec prayer,
            String dateKey,
            int weekday,
            ZonedDateTime fireAt,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            boolean isBefore,
            int minutesBefore,
            List<NativePushClient.PushMessage> batch,
            List<PendingDelivery> pending
    ) {
        Object kindRaw = prefMap.get(kind);
        if (!(kindRaw instanceof Map<?, ?> kindMap)) {
            return;
        }
        if (!asBoolean(kindMap.get("enabled"), false)) {
            return;
        }
        if (!isWeekdayEnabled(prefMap.get("days"), weekday)) {
            return;
        }
        if (fireAt.isBefore(windowStart) || fireAt.isAfter(windowEnd)) {
            return;
        }
        if (deliveryRepository.existsByDeviceIdAndPrayerDateAndPrayerIdAndKind(
                device.getDeviceId(), dateKey, prayer.id(), kind
        )) {
            return;
        }

        int melodyIndex = asInt(kindMap.get("melodyIndex"), isBefore ? 0 : 1);
        String title = isBefore
                ? prayer.label() + " yaklaşıyor"
                : prayer.label() + " Vakti";
        String body = isBefore
                ? prayer.label() + " ezanına " + minutesBefore + " dakika kaldı"
                : prayer.label() + " ezanı okunuyor";

        Map<String, String> data = new HashMap<>();
        data.put("type", "prayer");
        data.put("prayerId", prayer.id());
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
        pending.add(new PendingDelivery(device.getDeviceId(), dateKey, prayer.id(), kind));
    }

    private void markDelivered(PendingDelivery delivery) {
        try {
            deliveryRepository.save(new PrayerPushDeliveryEntity(
                    UUID.randomUUID(),
                    delivery.deviceId(),
                    delivery.prayerDate(),
                    delivery.prayerId(),
                    delivery.kind()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent duplicate
        }
    }

    private void handlePushError(String deviceId, NativePushClient.PushTicket ticket) {
        String code = ticket.errorCode() != null ? ticket.errorCode() : "";
        tokenRepository.findByDeviceId(deviceId).ifPresent(entity -> {
            entity.setLastError(code + (ticket.message() != null ? (": " + ticket.message()) : ""));
            if (isInvalidToken(code)) {
                entity.setEnabled(false);
            }
            tokenRepository.save(entity);
        });
    }

    private static boolean isInvalidToken(String code) {
        String normalized = code == null ? "" : code.toUpperCase();
        return normalized.contains("UNREGISTERED")
                || normalized.contains("INVALID_ARGUMENT")
                || normalized.contains("INVALIDTOKEN")
                || normalized.contains("BADDEVICETOKEN")
                || normalized.contains("DEVICETOKENNOTFORTOPIC")
                || normalized.contains("UNREGISTERED");
    }

    private static String timeFor(PrayerDayDto day, String key) {
        return switch (key) {
            case "imsak" -> day.imsak();
            case "gunes" -> day.gunes();
            case "ogle" -> day.ogle();
            case "ikindi" -> day.ikindi();
            case "aksam" -> day.aksam();
            case "yatsi" -> day.yatsi();
            default -> null;
        };
    }

    private static LocalTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            String[] parts = text.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return LocalTime.of(hour, minute);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean isWeekdayEnabled(Object daysRaw, int sundayBasedWeekday) {
        if (!(daysRaw instanceof List<?> days) || days.size() < 7) {
            return true;
        }
        Object value = days.get(sundayBasedWeekday);
        return asBoolean(value, true);
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

    private static String coordKey(double lat, double lon) {
        return Math.round(lat * 1000) + ":" + Math.round(lon * 1000);
    }

    private record PrayerSpec(String id, String label, String timeKey) {
    }

    private record PendingDelivery(String deviceId, String prayerDate, String prayerId, String kind) {
    }
}
