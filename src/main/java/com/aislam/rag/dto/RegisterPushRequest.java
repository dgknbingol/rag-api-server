package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record RegisterPushRequest(
        @NotBlank String deviceId,
        @NotBlank String pushToken,
        @NotBlank String platform,
        @NotNull Double latitude,
        @NotNull Double longitude,
        /** prayerId -> prefs */
        @NotNull Map<String, Object> prayers,
        /** dailyKind -> prefs (opsiyonel; yoksa default) */
        Map<String, Object> daily,
        /** competitionKind -> prefs (opsiyonel) */
        Map<String, Object> competition,
        String timezone
) {
}
