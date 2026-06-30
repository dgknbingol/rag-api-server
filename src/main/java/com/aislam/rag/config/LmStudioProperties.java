package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "lmstudio")
public record LmStudioProperties(
        String baseUrl,
        String chatModel,
        String embeddingModel,
        Duration connectTimeout,
        Duration readTimeout,
        int maxTokens,
        double chatTemperature
) {
}
