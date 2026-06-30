package com.aislam.rag.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurkishTextNormalizerTest {

    @Test
    void normalizesTurkishCharactersAndPunctuation() {
        assertEquals("namazın farzları", TurkishTextNormalizer.normalize("NAMAZIN farzları!!!"));
        assertEquals("islam ın şartları", TurkishTextNormalizer.normalize("İslam'ın şartları"));
    }

    @Test
    void extractsKeywordsFromQuestion() {
        var keywords = TurkishTextNormalizer.extractKeywords(
                "Abdesti bozan durumlar nelerdir?",
                3,
                Set.of("nelerdir", "nedir")
        );

        assertTrue(keywords.contains("abdesti"));
        assertTrue(keywords.contains("bozan"));
        assertTrue(keywords.contains("durumlar"));
    }

    @Test
    void extractsPhrasesFromQuestion() {
        var phrases = TurkishTextNormalizer.extractPhrases(
                "Namazın farzları nelerdir?",
                2,
                4,
                3,
                Set.of("nelerdir", "nedir")
        );

        assertTrue(phrases.contains("namazın farzları"));
    }

    @Test
    void countsTermOccurrencesWithWordBoundaries() {
        String content = TurkishTextNormalizer.normalize("Namaz vakitleri ve namaz farzları");

        assertEquals(2, TurkishTextNormalizer.countTermOccurrences(content, "namaz"));
    }

    @Test
    void termMatchesAllowsShortTurkishSuffixes() {
        String content = TurkishTextNormalizer.normalize("Namazın farzları anlatılır.");

        assertTrue(TurkishTextNormalizer.termMatches(content, "namaz"));
        assertFalse(TurkishTextNormalizer.termMatches(content, "abdest"));
    }
}
