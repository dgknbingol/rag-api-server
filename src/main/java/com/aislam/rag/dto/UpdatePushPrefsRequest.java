package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdatePushPrefsRequest(
        @NotBlank String deviceId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Map<String, Object> prayers,
        Map<String, Object> daily,
        Map<String, Object> competition,
        String timezone
) {
}
