package com.aislam.rag.dto;

public record QuizParticipationResponse(
        boolean participated,
        boolean prizeEligibleAtJoin
) {
}
