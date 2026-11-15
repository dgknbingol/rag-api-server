package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.play-integrity")
public record PlayIntegrityProperties(
        /** true iken Android device-session Integrity token ister. */
        boolean enabled,
        String packageName,
        /** PLAY_RECOGNIZED zorunlu mu (Play Store dağıtımı). Preview APK için false. */
        boolean requireRecognized,
        /** MEETS_DEVICE_INTEGRITY zorunlu mu. */
        boolean requireDeviceIntegrity,
        /** Service account JSON (yoksa FIREBASE_CREDENTIALS_JSON kullanılır). */
        String credentialsJson
) {
    public PlayIntegrityProperties {
        if (packageName == null || packageName.isBlank()) {
            packageName = "net.eislam.app";
        }
    }

    public boolean credentialsConfigured(String firebaseFallbackJson) {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return true;
        }
        return firebaseFallbackJson != null && !firebaseFallbackJson.isBlank();
    }

    public String resolveCredentialsJson(String firebaseFallbackJson) {
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            return credentialsJson;
        }
        return firebaseFallbackJson == null ? "" : firebaseFallbackJson;
    }
}
