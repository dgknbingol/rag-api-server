package com.aislam.rag.dto;

import java.util.UUID;

public record RegisterQuizPlayerRequest(
        UUID playerId,
        String displayName
) {
}
