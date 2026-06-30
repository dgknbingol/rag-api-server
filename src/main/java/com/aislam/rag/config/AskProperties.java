package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ask")
public record AskProperties(
        String pipeline,
        String llm,
        String embedding,
        String systemPrompt
) {
    public AskProperties {
        if (pipeline == null || pipeline.isBlank()) {
            pipeline = "direct";
        }
        if (llm == null || llm.isBlank()) {
            llm = "deepseek";
        }
        if (embedding == null || embedding.isBlank()) {
            embedding = "local";
        }
    }
}
