package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.push")
public record PushProperties(
        boolean enabled,
        int lookaheadSeconds,
        int lookbackSeconds,
        int batchSize,
        /** Firebase service account JSON (tek satır veya raw). Boşsa Android FCM kapalı. */
        String firebaseCredentialsJson,
        /** APNs .p8 içeriği (PEM). Boşsa iOS push kapalı. */
        String apnsKeyPem,
        String apnsKeyId,
        String apnsTeamId,
        String apnsBundleId,
        boolean apnsProduction
) {
    public PushProperties {
        if (lookaheadSeconds <= 0) {
            lookaheadSeconds = 75;
        }
        if (lookbackSeconds < 0) {
            lookbackSeconds = 30;
        }
        if (batchSize <= 0 || batchSize > 500) {
            batchSize = 500;
        }
        if (firebaseCredentialsJson == null) {
            firebaseCredentialsJson = "";
        }
        if (apnsKeyPem == null) {
            apnsKeyPem = "";
        }
        if (apnsKeyId == null) {
            apnsKeyId = "";
        }
        if (apnsTeamId == null) {
            apnsTeamId = "";
        }
        if (apnsBundleId == null || apnsBundleId.isBlank()) {
            apnsBundleId = "net.eislam.app";
        }
    }

    public boolean firebaseConfigured() {
        return firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank();
    }

    public boolean apnsConfigured() {
        return apnsKeyPem != null && !apnsKeyPem.isBlank()
                && apnsKeyId != null && !apnsKeyId.isBlank()
                && apnsTeamId != null && !apnsTeamId.isBlank();
    }
}
