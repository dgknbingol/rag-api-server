package com.aislam.rag.service;

import com.aislam.rag.domain.AggregatedChunk;
import com.aislam.rag.domain.SectionParagraph;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ParagraphChunkAggregator {

    public List<String> aggregate(List<String> paragraphs, int targetChunkSize, int maxChunkSize, int overlapChars) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return List.of();
        }

        List<SectionParagraph> sectionParagraphs = paragraphs.stream()
                .map(paragraph -> new SectionParagraph(null, paragraph))
                .toList();
        return aggregateSectionParagraphs(sectionParagraphs, targetChunkSize, maxChunkSize, overlapChars).stream()
                .map(AggregatedChunk::content)
                .toList();
    }

    public List<AggregatedChunk> aggregateSectionParagraphs(
            List<SectionParagraph> paragraphs,
            int targetChunkSize,
            int maxChunkSize,
            int overlapChars
    ) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            return List.of();
        }

        List<SectionParagraph> propagatedParagraphs = propagateSectionTitles(paragraphs);
        List<AggregatedChunk> chunks = new ArrayList<>();
        List<SectionParagraph> buffer = new ArrayList<>();
        String runningSectionTitle = null;

        for (SectionParagraph paragraph : propagatedParagraphs) {
            if (paragraph.text() == null || paragraph.text().isBlank()) {
                continue;
            }

            if (paragraph.sectionTitle() != null && !paragraph.sectionTitle().isBlank()) {
                runningSectionTitle = paragraph.sectionTitle();
            }

            if (buffer.isEmpty()) {
                buffer.add(paragraph);
                continue;
            }

            String candidate = join(buffer, paragraph.text());
            if (candidate.length() <= targetChunkSize || candidate.length() <= maxChunkSize) {
                buffer.add(paragraph);
                continue;
            }

            chunks.add(toAggregatedChunk(buffer, runningSectionTitle));
            buffer = new ArrayList<>(collectOverlapParagraphs(buffer, overlapChars));
            if (buffer.stream().noneMatch(item -> item.text().equals(paragraph.text()))) {
                buffer.add(paragraph);
            }
            if (paragraph.sectionTitle() != null && !paragraph.sectionTitle().isBlank()) {
                runningSectionTitle = paragraph.sectionTitle();
            }
        }

        if (!buffer.isEmpty()) {
            chunks.add(toAggregatedChunk(buffer, runningSectionTitle));
        }

        return List.copyOf(chunks);
    }

    private List<SectionParagraph> propagateSectionTitles(List<SectionParagraph> paragraphs) {
        String runningSectionTitle = null;
        List<SectionParagraph> propagated = new ArrayList<>(paragraphs.size());

        for (SectionParagraph paragraph : paragraphs) {
            if (paragraph.sectionTitle() != null && !paragraph.sectionTitle().isBlank()) {
                runningSectionTitle = paragraph.sectionTitle();
            }
            propagated.add(new SectionParagraph(runningSectionTitle, paragraph.text()));
        }

        return propagated;
    }

    private AggregatedChunk toAggregatedChunk(List<SectionParagraph> buffer, String fallbackSectionTitle) {
        String sectionTitle = resolveSectionTitle(buffer);
        if (sectionTitle == null || sectionTitle.isBlank()) {
            sectionTitle = fallbackSectionTitle;
        }
        return new AggregatedChunk(join(buffer), sectionTitle);
    }

    private String resolveSectionTitle(List<SectionParagraph> buffer) {
        for (SectionParagraph paragraph : buffer) {
            if (paragraph.sectionTitle() != null && !paragraph.sectionTitle().isBlank()) {
                return paragraph.sectionTitle();
            }
        }
        return null;
    }

    private AggregatedChunk toAggregatedChunk(List<SectionParagraph> buffer) {
        return toAggregatedChunk(buffer, null);
    }

    private List<SectionParagraph> collectOverlapParagraphs(List<SectionParagraph> paragraphs, int overlapChars) {
        if (overlapChars <= 0 || paragraphs.isEmpty()) {
            return List.of(paragraphs.getLast());
        }

        List<SectionParagraph> overlap = new ArrayList<>();
        int usedChars = 0;

        for (int i = paragraphs.size() - 1; i >= 0; i--) {
            SectionParagraph paragraph = paragraphs.get(i);
            int paragraphLength = paragraph.text().length();
            int separatorLength = overlap.isEmpty() ? 0 : 2;

            if (overlap.isEmpty() || usedChars + separatorLength + paragraphLength <= overlapChars) {
                overlap.addFirst(paragraph);
                usedChars += separatorLength + paragraphLength;
            } else {
                break;
            }
        }

        return overlap.isEmpty() ? List.of(paragraphs.getLast()) : overlap;
    }

    private String join(List<SectionParagraph> paragraphs) {
        return String.join("\n\n", paragraphs.stream().map(SectionParagraph::text).toList());
    }

    private String join(List<SectionParagraph> paragraphs, String nextParagraph) {
        return join(paragraphs) + "\n\n" + nextParagraph;
    }
}
