package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.app-attest")
public record AppAttestProperties(
        /** true iken iOS device-session App Attest ister. */
        boolean enabled,
        /** Apple Team ID (APNS_TEAM_ID ile aynı olabilir). */
        String teamId,
        String bundleId,
        /**
         * true = App Store / TestFlight (appattest).
         * false = Xcode development (appattestdevelop).
         */
        boolean production
) {
    public AppAttestProperties {
        if (teamId == null) {
            teamId = "";
        }
        if (bundleId == null || bundleId.isBlank()) {
            bundleId = "net.eislam.app";
        }
    }

    public boolean teamConfigured() {
        return teamId != null && !teamId.isBlank();
    }
}
