package com.aislam.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuizSeedOptionDto(
        String label,
        String text,
        @JsonProperty("correct") boolean correct
) {
}
