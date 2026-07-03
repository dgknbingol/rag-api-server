package com.aislam.rag.dto;

import java.util.UUID;

public record QuizAnswerCheckResponse(
        boolean correct,
        UUID correctOptionId,
        int points
) {
}
