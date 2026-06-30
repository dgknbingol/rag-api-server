package com.aislam.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextCleanerTest {

    private PdfTextCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new PdfTextCleaner();
    }

    @Test
    void collapsesExcessiveWhitespaceAndPreservesParagraphs() {
        String input = "Müslüman   olmak   için\n\n\n\nİslam şartları";
        String cleaned = cleaner.clean(input);

        assertEquals("Müslüman olmak için\n\nİslam şartları", cleaned);
    }

    @Test
    void fixesHyphenatedWordsAcrossLineBreaks() {
        String input = "Farz olan na-\nmaz vakitleri bellidir.";
        String cleaned = cleaner.clean(input);

        assertEquals("Farz olan namaz vakitleri bellidir.", cleaned);
    }

    @Test
    void mergesWrappedLinesInsideParagraph() {
        String input = "Bu paragraf PDF satır\nkırılması nedeniyle bölünmüştür.";
        String cleaned = cleaner.clean(input);

        assertEquals("Bu paragraf PDF satır kırılması nedeniyle bölünmüştür.", cleaned);
    }

    @Test
    void removesRepeatedPageHeadersAcrossPages() {
        List<String> pages = List.of(
                "NAMAZ İLMİHALİ\nİslam'ın şartları bestir.",
                "NAMAZ İLMİHALİ\nNamaz kılmak farzdır.",
                "NAMAZ İLMİHALİ\nOruç tutmak farzdır."
        );

        Set<String> repeatedHeaders = cleaner.detectRepeatedHeaders(pages);
        assertTrue(repeatedHeaders.contains("NAMAZ İLMİHALİ"));

        String cleaned = cleaner.clean(pages.getFirst(), repeatedHeaders);
        assertFalse(cleaned.contains("NAMAZ İLMİHALİ"));
        assertTrue(cleaned.contains("İslam'ın şartları bestir."));
    }

    @Test
    void preservesTurkishCharacters() {
        String input = "Müslüman İslam Şehadet oruç ğüşıöç";
        assertEquals(input, cleaner.clean(input));
    }

    @Test
    void keepsParagraphBoundariesAfterCleaning() {
        String input = "Birinci paragraf devam\neder.\n\nİkinci paragraf ayrıdır.";
        String cleaned = cleaner.clean(input);

        assertEquals("Birinci paragraf devam eder.\n\nİkinci paragraf ayrıdır.", cleaned);
    }
}
