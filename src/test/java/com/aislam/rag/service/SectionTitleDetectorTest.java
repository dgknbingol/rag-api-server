package com.aislam.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionTitleDetectorTest {

    private SectionTitleDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SectionTitleDetector();
    }

    @Test
    void detectsUppercaseSectionTitles() {
        assertTrue(detector.isSectionTitle("ABDESTİ BOZAN HALLER"));
        assertTrue(detector.isSectionTitle("NAMAZIN FARZLARI"));
    }

    @Test
    void detectsNumberedSectionTitles() {
        assertTrue(detector.isSectionTitle("1. Abdestin Farzları"));
        assertTrue(detector.isSectionTitle("a) Mesh"));
        assertTrue(detector.isSectionTitle("2) Oruç"));
    }

    @Test
    void rejectsRegularParagraphLines() {
        assertFalse(detector.isSectionTitle("Abdest almak için su kullanılır ve yüz yıkanır."));
    }
}
