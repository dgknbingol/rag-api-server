package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.dto.RegisterPushRequest;
import com.aislam.rag.dto.SendPushRequest;
import com.aislam.rag.dto.TestPushRequest;
import com.aislam.rag.dto.UnregisterPushRequest;
import com.aislam.rag.dto.UpdatePushPrefsRequest;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.DevicePushTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/notifications", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class NotificationPushController {

    private final DevicePushTokenService devicePushTokenService;
    private final PushProperties pushProperties;

    public NotificationPushController(
            DevicePushTokenService devicePushTokenService,
            PushProperties pushProperties
    ) {
        this.devicePushTokenService = devicePushTokenService;
        this.pushProperties = pushProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.register(request));
    }

    @PutMapping("/prefs")
    public ResponseEntity<Map<String, Object>> updatePrefs(@Valid @RequestBody UpdatePushPrefsRequest request) {
        return ResponseEntity.ok(devicePushTokenService.updatePrefs(request));
    }

    @DeleteMapping("/register")
    public ResponseEntity<Map<String, Object>> unregister(@Valid @RequestBody UnregisterPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.unregister(request));
    }

    /** QA: anında test push. */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@Valid @RequestBody TestPushRequest request) {
        return ResponseEntity.ok(devicePushTokenService.sendTest(request));
    }

    /**
     * Admin manuel push. Authorization = {@code PUSH_ADMIN_AUTH}.
     * Body: title, body + (deviceId | deviceIds | all=true).
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SendPushRequest request
    ) {
        requireAdminAuthorization(authorization);
        return ResponseEntity.ok(devicePushTokenService.sendManual(request));
    }

    private void requireAdminAuthorization(String authorization) {
        if (!pushProperties.adminAuthorizationConfigured()) {
            throw new RagException(
                    "PUSH_ADMIN_AUTH yapılandırılmamış.",
                    "PUSH_ADMIN_NOT_CONFIGURED"
            );
        }
        if (!authorizationMatches(authorization, pushProperties.adminAuthorization())) {
            throw new RagException("Push admin yetkisiz.", "PUSH_UNAUTHORIZED");
        }
    }

    private static boolean authorizationMatches(String received, String expected) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        if (received == null) {
            return false;
        }
        String r = received.trim();
        String e = expected.trim();
        if (r.equals(e)) {
            return true;
        }
        return stripBearerPrefix(r).equals(stripBearerPrefix(e));
    }

    private static String stripBearerPrefix(String value) {
        String current = value.trim();
        while (true) {
            if (current.length() >= 7 && current.regionMatches(true, 0, "Bearer ", 0, 7)) {
                current = current.substring(7).trim();
                continue;
            }
            if (current.length() > 6
                    && current.regionMatches(true, 0, "Bearer", 0, 6)
                    && !Character.isWhitespace(current.charAt(6))) {
                current = current.substring(6).trim();
                continue;
            }
            break;
        }
        return current;
    }
}
