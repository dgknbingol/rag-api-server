package com.aislam.rag.service;

import com.aislam.rag.client.NativePushClient;
import com.aislam.rag.dto.RegisterPushRequest;
import com.aislam.rag.dto.TestPushRequest;
import com.aislam.rag.dto.UnregisterPushRequest;
import com.aislam.rag.dto.UpdatePushPrefsRequest;
import com.aislam.rag.entity.DevicePushTokenEntity;
import com.aislam.rag.repository.DevicePushTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DevicePushTokenService {

    private static final String DEFAULT_TZ = "Europe/Istanbul";

    private final DevicePushTokenRepository repository;
    private final DistrictResolverService districtResolver;
    private final NativePushClient nativePushClient;

    public DevicePushTokenService(
            DevicePushTokenRepository repository,
            DistrictResolverService districtResolver,
            NativePushClient nativePushClient
    ) {
        this.repository = repository;
        this.districtResolver = districtResolver;
        this.nativePushClient = nativePushClient;
    }

    @Transactional
    public Map<String, Object> register(RegisterPushRequest request) {
        String timezone = blankToDefault(request.timezone(), DEFAULT_TZ);
        String districtId = districtResolver.resolveDistrictId(request.latitude(), request.longitude());
        Map<String, Object> prayerPrefs = normalizeOrDefault(request.prayers(), defaultPrayerPrefs());
        Map<String, Object> dailyPrefs = normalizeOrDefault(request.daily(), defaultDailyPrefs());
        Map<String, Object> competitionPrefs = normalizeOrDefault(request.competition(), defaultCompetitionPrefs());
        String platform = request.platform().trim().toLowerCase();

        DevicePushTokenEntity entity = repository.findByDeviceId(request.deviceId())
                .orElseGet(() -> new DevicePushTokenEntity(
                        UUID.randomUUID(),
                        request.deviceId().trim(),
                        request.pushToken().trim(),
                        platform,
                        request.latitude(),
                        request.longitude(),
                        districtId,
                        timezone,
                        prayerPrefs
                ));

        entity.setPushToken(request.pushToken().trim());
        entity.setPlatform(platform);
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setDistrictId(districtId);
        entity.setTimezone(timezone);
        entity.setPrayerPrefs(prayerPrefs);
        entity.setDailyPrefs(dailyPrefs);
        entity.setCompetitionPrefs(competitionPrefs);
        entity.setEnabled(true);
        entity.setLastError(null);

        repository.save(entity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("deviceId", entity.getDeviceId());
        response.put("districtId", entity.getDistrictId());
        return response;
    }

    @Transactional
    public Map<String, Object> updatePrefs(UpdatePushPrefsRequest request) {
        DevicePushTokenEntity entity = repository.findByDeviceId(request.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cihaz kaydı bulunamadı."));

        String timezone = blankToDefault(request.timezone(), entity.getTimezone());
        String districtId = districtResolver.resolveDistrictId(request.latitude(), request.longitude());

        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setDistrictId(districtId);
        entity.setTimezone(timezone);
        entity.setPrayerPrefs(normalizeOrDefault(request.prayers(), defaultPrayerPrefs()));
        if (request.daily() != null) {
            entity.setDailyPrefs(normalizeOrDefault(request.daily(), defaultDailyPrefs()));
        }
        if (request.competition() != null) {
            entity.setCompetitionPrefs(normalizeOrDefault(request.competition(), defaultCompetitionPrefs()));
        }
        entity.setEnabled(true);
        entity.setLastError(null);
        repository.save(entity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("districtId", districtId);
        return response;
    }

    @Transactional
    public Map<String, Object> unregister(UnregisterPushRequest request) {
        repository.findByDeviceId(request.deviceId()).ifPresent(entity -> {
            entity.setEnabled(false);
            repository.save(entity);
        });
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sendTest(TestPushRequest request) {
        DevicePushTokenEntity entity = repository.findByDeviceId(request.deviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cihaz kaydı bulunamadı."));

        String title = blankToDefault(request.title(), "e-İslam test");
        String body = blankToDefault(request.body(), "Sunucu push testi");

        var ticket = nativePushClient.send(List.of(
                new NativePushClient.PushMessage(
                        entity.getPushToken(),
                        entity.getPlatform(),
                        title,
                        body,
                        Map.of("type", "prayer", "openHome", "true", "test", "true"),
                        "notif-sound-v3-1"
                )
        )).stream().findFirst().orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", ticket != null && ticket.ok());
        if (ticket != null) {
            response.put("errorCode", ticket.errorCode());
            response.put("message", ticket.message());
        }
        return response;
    }

    private Map<String, Object> normalizeOrDefault(Map<String, Object> incoming, Map<String, Object> defaults) {
        if (incoming == null || incoming.isEmpty()) {
            return defaults;
        }
        return new LinkedHashMap<>(incoming);
    }

    private Map<String, Object> defaultPrayerPrefs() {
        Map<String, Object> all = new LinkedHashMap<>();
        for (String id : List.of("imsak", "gunes", "ogle", "ikindi", "aksam", "yatsi")) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("atTime", Map.of("enabled", true, "melodyIndex", id.equals("gunes") ? 0 : 1));
            one.put("before", Map.of("enabled", true, "melodyIndex", 0, "minutesBefore", 15));
            one.put("days", List.of(true, true, true, true, true, true, true));
            all.put(id, one);
        }
        return all;
    }

    private Map<String, Object> defaultDailyPrefs() {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("ayet", dailyOne(true, 0, 10, 0, List.of(true, true, true, true, true, true, true)));
        all.put("dua", dailyOne(true, 0, 14, 0, List.of(true, true, true, true, true, true, true)));
        all.put("hadis", dailyOne(true, 0, 16, 0, List.of(true, true, true, true, true, true, true)));
        all.put("hutbe", dailyOne(true, 0, 12, 0, List.of(false, false, false, false, false, true, false)));
        all.put("asma", dailyOne(true, 0, 21, 0, List.of(true, true, true, true, true, true, true)));
        return all;
    }

    private static Map<String, Object> dailyOne(
            boolean enabled,
            int melodyIndex,
            int hour,
            int minute,
            List<Boolean> days
    ) {
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("enabled", enabled);
        one.put("melodyIndex", melodyIndex);
        one.put("hour", hour);
        one.put("minute", minute);
        one.put("days", days);
        return one;
    }

    private Map<String, Object> defaultCompetitionPrefs() {
        Map<String, Object> all = new LinkedHashMap<>();
        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("atTime", Map.of("enabled", true, "melodyIndex", 0));
        daily.put("before", Map.of("enabled", true, "melodyIndex", 0, "minutesBefore", 15));
        daily.put("days", List.of(true, true, true, true, true, true, true));
        all.put("daily", daily);
        return all;
    }

    private static String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
