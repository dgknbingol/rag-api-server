package com.aislam.rag.dto;

import java.util.List;

public record EducationModuleSeedDto(
    String id,
    String title,
    String summary,
    EducationQuizSeedDto quiz,
    List<EducationTopicSeedDto> lessons) {}
