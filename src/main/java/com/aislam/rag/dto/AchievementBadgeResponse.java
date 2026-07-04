package com.aislam.rag.dto;

public record AchievementBadgeResponse(
        String id,
        String title,
        String description,
        String icon,
        boolean unlocked
) {
}
