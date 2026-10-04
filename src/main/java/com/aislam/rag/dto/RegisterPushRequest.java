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
        /** prayerId -> prefs map (atTime/before/days) */
        @NotNull Map<String, Object> prayers,
        String timezone
) {
}
