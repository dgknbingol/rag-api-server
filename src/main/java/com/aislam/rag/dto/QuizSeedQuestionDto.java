package com.aislam.rag.dto;

import java.util.List;

public record QuizSeedQuestionDto(
        String category,
        String question,
        List<QuizSeedOptionDto> options
) {
}
