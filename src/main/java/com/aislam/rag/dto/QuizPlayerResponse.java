package com.aislam.rag.dto;

import java.util.UUID;

public record QuizPlayerResponse(
        UUID playerId,
        String displayName
) {
}
