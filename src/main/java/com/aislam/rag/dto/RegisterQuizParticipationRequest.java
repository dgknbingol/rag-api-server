package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterQuizParticipationRequest(
        @NotNull UUID playerId,
        @NotBlank String eventId
) {
}
