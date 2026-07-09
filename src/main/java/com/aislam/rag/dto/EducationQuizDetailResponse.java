package com.aislam.rag.dto;

import java.util.List;

public record EducationQuizOptionDto(String label, String text, boolean correct) {}

public record EducationQuizQuestionDto(String question, List<EducationQuizOptionDto> options) {}

public record EducationQuizDetailResponse(
    String id,
    String title,
    String moduleId,
    List<EducationQuizQuestionDto> questions) {}
