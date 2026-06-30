package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "qdrant")
public record QdrantProperties(
        String baseUrl,
        String collectionName,
        int vectorSize,
        Duration connectTimeout,
        Duration readTimeout
) {
}
