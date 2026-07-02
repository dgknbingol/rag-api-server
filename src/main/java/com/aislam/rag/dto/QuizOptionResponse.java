package com.aislam.rag.dto;

import com.aislam.rag.entity.QuizOptionEntity;

import java.util.UUID;

public record QuizOptionResponse(
        UUID id,
        String label,
        String text
) {

    public static QuizOptionResponse from(QuizOptionEntity option) {
        return new QuizOptionResponse(
                option.getId(),
                option.getOptionLabel(),
                option.getOptionText()
        );
    }
}
