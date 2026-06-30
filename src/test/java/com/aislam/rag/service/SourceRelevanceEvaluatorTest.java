package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.config.RerankingProperties;
import com.aislam.rag.domain.SourceChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceRelevanceEvaluatorTest {

    private SourceRelevanceEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SourceRelevanceEvaluator(RagProperties.forTests(), RerankingProperties.forTests());
    }

    @Test
    void acceptsChunkWithMatchingSectionTitleAndKeywords() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc", "İlmihal", "Diyanet",
                        "İslam'ın beş şartı kelime-i şehadet ile başlar.",
                        "İslam'ın Şartları", 10, 0, 0.82
                ).withRerankScores(0.10, 0.12, 0.20, 0.0, 1.04)
        );

        assertTrue(evaluator.isRelevantEnough("İslam'ın şartları nelerdir?", chunks));
    }

    @Test
    void acceptsWhenVectorScoreAboveThresholdEvenWithZeroKeywordScore() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "3", "doc", "İlmihal", "Diyanet",
                        "İman, Allah'ın varlığına ve birliğine inanmaktır.",
                        "I. İMAN", 5, 0, 0.60
                ).withRerankScores(0.0, 0.0, 0.0, 0.0, 0.60)
        );

        assertTrue(evaluator.isRelevantEnough("İslam nedir?", chunks));
    }

    @Test
    void rejectsWhenAllChunksBelowVectorThresholdAndNoKeywordMatch() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc", "Namaz İlmihali", "Diyanet",
                        "Cuma namazı farz-ı kifayedir.",
                        "Cuma Namazı", 40, 12, 0.40
                ).withRerankScores(0.0, 0.0, 0.0, 0.0, 0.40)
        );

        assertFalse(evaluator.isRelevantEnough("İslam'ın şartları nelerdir?", chunks));
    }

    @Test
    void acceptsAtVectorThresholdBoundary() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "4", "doc", "İlmihal", "Diyanet",
                        "İman bölümü metni.",
                        "I. İMAN", 3, 1, 0.55
                ).withRerankScores(0.0, 0.0, 0.0, 0.0, 0.55)
        );

        assertTrue(evaluator.isRelevantEnough("İslam nedir?", chunks));
    }
}
