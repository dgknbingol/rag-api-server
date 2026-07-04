package com.aislam.rag.dto;

public record MonthlyStatsResponse(
        String monthKey,
        String monthLabel,
        long totalScore,
        int rank,
        long totalPlayers,
        int quizzesCompleted,
        int bestDailyScore,
        boolean currentMonth
) {
}
