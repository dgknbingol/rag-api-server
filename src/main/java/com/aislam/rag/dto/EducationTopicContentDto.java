package com.aislam.rag.dto;

import java.util.List;

public record EducationTopicContentDto(
        Integer readingMinutes,
        List<EducationBlockDto> blocks
) {
}
