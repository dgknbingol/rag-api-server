package com.aislam.rag.security;

import com.aislam.rag.entity.UserEntity;
import com.aislam.rag.exception.RagException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Kimlik: cihaz JWT subject (= mobil app user id) veya giriş yapmış kullanıcı id.
 * İstemci {@code X-App-User-Id} varsa token ile eşleşmeli (spoof engeli).
 */
public final class RequestAppUserId {

    private RequestAppUserId() {
    }

    public static UUID require(String appUserIdHeader) {
        UUID fromToken = requireFromSecurityContext();
        if (appUserIdHeader != null && !appUserIdHeader.isBlank()) {
            UUID fromHeader;
            try {
                fromHeader = UUID.fromString(appUserIdHeader.trim());
            } catch (IllegalArgumentException ex) {
                throw new RagException("Geçersiz X-App-User-Id.", "APP_USER_ID_INVALID");
            }
            if (!fromToken.equals(fromHeader)) {
                throw new RagException("Token cihazı eşleşmiyor.", "DEVICE_TOKEN_MISMATCH");
            }
        }
        return fromToken;
    }

    public static UUID requireFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RagException("Oturum gerekli.", "DEVICE_TOKEN_REQUIRED");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID deviceId) {
            return deviceId;
        }
        if (principal instanceof UserEntity user) {
            return user.getId();
        }
        if (principal instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        throw new RagException("Oturum gerekli.", "DEVICE_TOKEN_REQUIRED");
    }
}
