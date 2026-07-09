package com.aislam.rag.dto;

import java.util.List;

public record EducationCatalogSeedDto(
        int version,
        List<EducationCategorySeedDto> categories
) {
}
