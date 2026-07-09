package com.aislam.rag.dto;

import java.util.List;

public record EducationModuleDto(
    String id,
    String title,
    String summary,
    EducationQuizDto quiz,
    List<EducationTopicSummaryDto> lessons) {}
