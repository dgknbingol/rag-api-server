package com.aislam.rag.dto;

import java.util.List;

public record EducationQuizQuestionDto(String question, List<EducationQuizOptionDto> options) {}
