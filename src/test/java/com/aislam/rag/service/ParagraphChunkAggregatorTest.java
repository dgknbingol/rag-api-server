package com.aislam.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParagraphChunkAggregatorTest {

    private ParagraphChunkAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ParagraphChunkAggregator();
    }

    @Test
    void keepsSectionTitleWithFollowingParagraphs() {
        List<String> paragraphs = List.of(
                "Namazin Farzlari",
                "paragraph1",
                "paragraph2"
        );

        List<String> chunks = aggregator.aggregate(paragraphs, 1000, 1500, 250);

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().contains("Namazin Farzlari"));
        assertTrue(chunks.getFirst().contains("paragraph1"));
        assertTrue(chunks.getFirst().contains("paragraph2"));
    }

    @Test
    void neverSplitsParagraphInMiddle() {
        String longParagraph = "A".repeat(1600);
        List<String> chunks = aggregator.aggregate(List.of(longParagraph), 1000, 1500, 250);

        assertEquals(1, chunks.size());
        assertEquals(longParagraph, chunks.getFirst());
    }

    @Test
    void overlapsPreviousParagraphOnly() {
        List<String> paragraphs = List.of(
                "alpha paragraph",
                "beta paragraph",
                "gamma paragraph"
        );

        List<String> chunks = aggregator.aggregate(paragraphs, 20, 40, 10);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(1).contains("beta paragraph"));
    }
}
