package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat")
public record ChatProperties(
        int freeDailyLimit,
        int premiumDailyLimit
) {
    public ChatProperties {
        if (freeDailyLimit <= 0) {
            freeDailyLimit = 3;
        }
        if (premiumDailyLimit <= 0) {
            premiumDailyLimit = 50;
        }
    }
}
