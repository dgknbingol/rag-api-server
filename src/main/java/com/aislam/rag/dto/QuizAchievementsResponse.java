package com.aislam.rag.dto;

import java.util.List;

public record QuizAchievementsResponse(
        List<MonthlyStatsResponse> monthlyHistory,
        AchievementHighlightsResponse highlights,
        List<AchievementBadgeResponse> badges
) {
}
