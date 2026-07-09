package com.aislam.rag.dto;

import java.util.List;

public record EducationCategorySeedDto(
    String id,
    String title,
    String subtitle,
    String icon,
    EducationQuizSeedDto quiz,
    List<EducationModuleSeedDto> modules) {}
