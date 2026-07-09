package com.aislam.rag.dto;

import java.util.List;

public record EducationQuizDetailResponse(
    String id,
    String title,
    String moduleId,
    List<EducationQuizQuestionDto> questions) {}
