package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        long jwtExpirationHours,
        String googleClientIds,
        String appleAudience
) {
    public AuthProperties {
        if (googleClientIds == null) {
            googleClientIds = "";
        }
        if (appleAudience == null || appleAudience.isBlank()) {
            appleAudience = "net.eislam.app";
        }
    }

    public List<String> googleClientIdList() {
        return Arrays.stream(googleClientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();
    }
}
