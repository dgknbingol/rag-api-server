package com.aislam.rag.util;

public final class QuizScoring {

    public static final int QUESTION_TIME_MS = 20_000;
    public static final int MAX_POINTS_PER_QUESTION = 100;

    private QuizScoring() {
    }

    public static long clampResponseTimeMs(long responseTimeMs) {
        return Math.min(Math.max(responseTimeMs, 0), QUESTION_TIME_MS);
    }

    public static int calculatePoints(boolean correct, long responseTimeMs) {
        if (!correct) {
            return 0;
        }

        long clampedMs = clampResponseTimeMs(responseTimeMs);
        double remainingRatio = (QUESTION_TIME_MS - clampedMs) / (double) QUESTION_TIME_MS;
        return (int) Math.round(MAX_POINTS_PER_QUESTION * remainingRatio);
    }
}
