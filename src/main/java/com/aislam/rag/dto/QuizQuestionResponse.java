package com.aislam.rag.dto;

import com.aislam.rag.entity.QuizOptionEntity;
import com.aislam.rag.entity.QuizQuestionEntity;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record QuizQuestionResponse(
        UUID id,
        String question,
        String category,
        List<QuizOptionResponse> options
) {

    public static QuizQuestionResponse from(QuizQuestionEntity entity) {
        List<QuizOptionResponse> options = entity.getOptions().stream()
                .sorted(Comparator.comparingInt(QuizOptionEntity::getSortOrder))
                .map(QuizOptionResponse::from)
                .toList();

        return new QuizQuestionResponse(
                entity.getId(),
                entity.getQuestionText(),
                entity.getCategory(),
                options
        );
    }
}
