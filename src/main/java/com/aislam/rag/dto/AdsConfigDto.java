package com.aislam.rag.dto;

/**
 * Uzaktan yönetilen reklam ayarları (mobil + quota).
 * Süreler milisaniye.
 */
public record AdsConfigDto(
        boolean bannerEnabled,
        boolean interstitialEnabled,
        boolean appOpenEnabled,
        boolean rewardedEnabled,
        long interstitialCooldownMs,
        long appOpenCooldownMs,
        int rewardedDailyMax
) {
    public static AdsConfigDto defaults() {
        return new AdsConfigDto(
                true,
                true,
                true,
                true,
                2 * 60 * 1000L,
                30 * 60 * 1000L,
                5
        );
    }
}
