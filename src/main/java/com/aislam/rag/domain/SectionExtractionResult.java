package com.aislam.rag.domain;

import java.util.List;

public record SectionExtractionResult(
        List<SectionParagraph> paragraphs,
        String lastSectionTitle
) {
}
