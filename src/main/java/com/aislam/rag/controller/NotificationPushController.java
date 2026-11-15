package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.dto.RegisterPushRequest;
import com.aislam.rag.dto.SendPushRequest;
import com.aislam.rag.dto.TestPushRequest;
import com.aislam.rag.dto.UnregisterPushRequest;
import com.aislam.rag.dto.UpdatePushPrefsRequest;
import com.aislam.rag.security.ApiSecretAuth;
import com.aislam.rag.security.DeviceSessionAuth;
import com.aislam.rag.security.JwtService;
import com.aislam.rag.service.DevicePushTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/notifications", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class NotificationPushController {

    private final DevicePushTokenService devicePushTokenService;
    private final PushProperties pushProperties;
    private final JwtService jwtService;

    public NotificationPushController(
            DevicePushTokenService devicePushTokenService,
            PushProperties pushProperties,
            JwtService jwtService
    ) {
        this.devicePushTokenService = devicePushTokenService;
        this.pushProperties = pushProperties;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody RegisterPushRequest request
    ) {
        requireDeviceSession(authorization, request.deviceId());
        return ResponseEntity.ok(devicePushTokenService.register(request));
    }

    @PutMapping("/prefs")
    public ResponseEntity<Map<String, Object>> updatePrefs(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UpdatePushPrefsRequest request
    ) {
        requireDeviceSession(authorization, request.deviceId());
        return ResponseEntity.ok(devicePushTokenService.updatePrefs(request));
    }

    @DeleteMapping("/register")
    public ResponseEntity<Map<String, Object>> unregister(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody UnregisterPushRequest request
    ) {
        requireDeviceSession(authorization, request.deviceId());
        return ResponseEntity.ok(devicePushTokenService.unregister(request));
    }

    /** QA test push — sadece admin (Postman). Mobil APK’dan çağrılmaz. */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody TestPushRequest request
    ) {
        requireAdminAuthorization(authorization);
        return ResponseEntity.ok(devicePushTokenService.sendTest(request));
    }

    /**
     * Admin manuel / broadcast push.
     * Authorization = {@code PUSH_ADMIN_AUTH}.
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SendPushRequest request
    ) {
        requireAdminAuthorization(authorization);
        return ResponseEntity.ok(devicePushTokenService.sendManual(request));
    }

    private void requireDeviceSession(String authorization, String bodyDeviceId) {
        UUID deviceId = DeviceSessionAuth.requireDeviceId(jwtService, authorization);
        DeviceSessionAuth.requireDeviceMatches(deviceId, bodyDeviceId);
    }

    private void requireAdminAuthorization(String authorization) {
        ApiSecretAuth.requireConfigured(
                pushProperties.adminAuthorizationConfigured(),
                "PUSH_ADMIN_NOT_CONFIGURED",
                "PUSH_ADMIN_AUTH yapılandırılmamış."
        );
        ApiSecretAuth.requireAuthorizationMatch(
                authorization,
                pushProperties.adminAuthorization(),
                "PUSH_UNAUTHORIZED",
                "Push admin yetkisiz."
        );
    }
}
