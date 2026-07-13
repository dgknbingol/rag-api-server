package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat")
public record ChatProperties(
        int freeDailyLimit,
        int premiumDailyLimit,
        int maxContextMessages,
        boolean jobsEnabled,
        long jobPollIntervalMs,
        int jobBatchSize
) {
    public ChatProperties {
        if (freeDailyLimit <= 0) {
            freeDailyLimit = 3;
        }
        if (premiumDailyLimit <= 0) {
            premiumDailyLimit = 50;
        }
        if (maxContextMessages <= 0) {
            maxContextMessages = 20;
        }
        if (jobPollIntervalMs <= 0) {
            jobPollIntervalMs = 1500;
        }
        if (jobBatchSize <= 0) {
            jobBatchSize = 2;
        }
    }
}
