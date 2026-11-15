package com.aislam.rag.security;

import com.aislam.rag.exception.RagException;
import org.springframework.http.HttpHeaders;

import java.util.UUID;

/** Mobil cihaz JWT (Bearer) doğrulama — APK’da secret yok. */
public final class DeviceSessionAuth {

    private DeviceSessionAuth() {
    }

    public static UUID requireDeviceId(JwtService jwtService, String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new RagException("Cihaz oturumu gerekli.", "DEVICE_TOKEN_REQUIRED");
        }
        String value = authorization.trim();
        if (!value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new RagException("Cihaz oturumu gerekli.", "DEVICE_TOKEN_REQUIRED");
        }
        String token = value.substring(7).trim();
        if (token.isEmpty()) {
            throw new RagException("Cihaz oturumu gerekli.", "DEVICE_TOKEN_REQUIRED");
        }
        try {
            return jwtService.parseDeviceId(token);
        } catch (RagException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new RagException("Geçersiz veya süresi dolmuş cihaz tokenı.", "DEVICE_TOKEN_INVALID");
        }
    }

    public static void requireDeviceMatches(UUID tokenDeviceId, String bodyDeviceId) {
        if (bodyDeviceId == null || bodyDeviceId.isBlank()) {
            throw new RagException("deviceId gerekli.", "DEVICE_ID_REQUIRED");
        }
        UUID bodyId;
        try {
            bodyId = UUID.fromString(bodyDeviceId.trim());
        } catch (IllegalArgumentException ex) {
            throw new RagException("Geçersiz deviceId.", "DEVICE_ID_INVALID");
        }
        if (!tokenDeviceId.equals(bodyId)) {
            throw new RagException("Token cihazı eşleşmiyor.", "DEVICE_TOKEN_MISMATCH");
        }
    }

    public static String authorizationHeaderName() {
        return HttpHeaders.AUTHORIZATION;
    }
}
