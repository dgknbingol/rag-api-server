package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ads-config")
public record AdsConfigProperties(
        /** PUT /api/config/ads için admin anahtarı (X-Config-Admin-Key). */
        String adminKey
) {
    public boolean isAdminConfigured() {
        return adminKey != null && !adminKey.isBlank();
    }
}
