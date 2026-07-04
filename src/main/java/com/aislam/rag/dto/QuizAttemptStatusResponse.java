package com.aislam.rag.dto;

public record QuizAttemptStatusResponse(
        boolean participated,
        boolean completed,
        int score,
        int correctCount,
        int questionCount
) {
    public static QuizAttemptStatusResponse empty() {
        return new QuizAttemptStatusResponse(false, false, 0, 0, 0);
    }
}
