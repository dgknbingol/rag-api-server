package com.aislam.rag.service;

import com.aislam.rag.domain.SectionExtractionResult;
import com.aislam.rag.domain.SectionParagraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SectionParagraphExtractor {

    private final SectionTitleDetector sectionTitleDetector;

    public SectionParagraphExtractor(SectionTitleDetector sectionTitleDetector) {
        this.sectionTitleDetector = sectionTitleDetector;
    }

    public SectionExtractionResult extract(String pageText) {
        return extract(pageText, null);
    }

    public SectionExtractionResult extract(String pageText, String inheritedSectionTitle) {
        if (pageText == null || pageText.isBlank()) {
            return new SectionExtractionResult(List.of(), inheritedSectionTitle);
        }

        List<SectionParagraph> paragraphs = new ArrayList<>();
        String currentSectionTitle = inheritedSectionTitle;
        StringBuilder paragraphBuffer = new StringBuilder();

        for (String rawLine : pageText.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                flushParagraph(paragraphs, currentSectionTitle, paragraphBuffer);
                continue;
            }

            if (sectionTitleDetector.isSectionTitle(line)) {
                flushParagraph(paragraphs, currentSectionTitle, paragraphBuffer);
                currentSectionTitle = line;
                continue;
            }

            if (!paragraphBuffer.isEmpty()) {
                paragraphBuffer.append("\n");
            }
            paragraphBuffer.append(line);
        }

        flushParagraph(paragraphs, currentSectionTitle, paragraphBuffer);
        return new SectionExtractionResult(List.copyOf(paragraphs), currentSectionTitle);
    }

    private void flushParagraph(
            List<SectionParagraph> paragraphs,
            String sectionTitle,
            StringBuilder paragraphBuffer
    ) {
        if (paragraphBuffer.isEmpty()) {
            return;
        }
        paragraphs.add(new SectionParagraph(sectionTitle, paragraphBuffer.toString().strip()));
        paragraphBuffer.setLength(0);
    }
}
