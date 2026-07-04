package com.aislam.rag.dto;

import java.util.UUID;

public record SubmitQuizAttemptRequest(
        UUID playerId,
        String eventId,
        int score,
        int correctCount,
        int questionCount
) {
}
