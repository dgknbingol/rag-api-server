package com.aislam.rag.dto;

import java.util.List;

public record AchievementHighlightsResponse(
        int currentStreakDays,
        int bestRankThisYear,
        long lifetimeScore,
        int quizzesThisMonth
) {
}
