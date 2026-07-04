package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.revenuecat")
public record RevenueCatProperties(
        String webhookAuthorization,
        String premiumEntitlementId
) {
    public RevenueCatProperties {
        if (premiumEntitlementId == null || premiumEntitlementId.isBlank()) {
            premiumEntitlementId = "premium";
        }
    }

    public boolean isWebhookConfigured() {
        return webhookAuthorization != null && !webhookAuthorization.isBlank();
    }
}
