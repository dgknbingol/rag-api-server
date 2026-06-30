package com.aislam.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "app.daily")
public record DailyProperties(
        String timezone
) {
    public ZoneId zoneId() {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("Europe/Istanbul");
        }
        return ZoneId.of(timezone);
    }
}
