package com.aislam.rag.dto;

public record EducationQuizDto(
    String id,
    String title,
    String type,
    int questionCount,
    int passPercent,
    String achievementId) {}
