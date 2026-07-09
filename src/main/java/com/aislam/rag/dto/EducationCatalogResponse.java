package com.aislam.rag.dto;

import java.util.List;

public record EducationCatalogResponse(
        int version,
        List<EducationCategoryDto> categories
) {
}
