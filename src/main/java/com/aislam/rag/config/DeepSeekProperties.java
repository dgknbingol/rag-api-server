package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "deepseek")
public record DeepSeekProperties(
        String baseUrl,
        String apiKey,
        String model,
        String systemPrompt,
        Duration connectTimeout,
        Duration readTimeout,
        int maxTokens,
        double temperature
) {
}
