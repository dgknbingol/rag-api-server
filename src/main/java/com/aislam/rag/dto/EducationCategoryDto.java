package com.aislam.rag.dto;

import java.util.List;

public record EducationCategoryDto(
    String id,
    String title,
    String subtitle,
    String icon,
    EducationQuizDto quiz,
    List<EducationModuleDto> modules) {}
