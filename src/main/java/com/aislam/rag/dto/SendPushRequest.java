package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Manuel push: tek cihaz, cihaz listesi veya tüm kayıtlı (enabled) cihazlar.
 * Hedef: {@code deviceId} | {@code deviceIds} | {@code all=true} — biri zorunlu.
 */
public record SendPushRequest(
        @NotBlank String title,
        @NotBlank String body,
        String deviceId,
        List<String> deviceIds,
        Boolean all,
        /** Android FCM channel id; boşsa notif-sound-v3-1. Ezan prefs ile aynı kanalı denemek için. */
        String androidChannelId
) {
}
