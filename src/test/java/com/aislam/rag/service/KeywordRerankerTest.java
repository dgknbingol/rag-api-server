package com.aislam.rag.service;

import com.aislam.rag.config.RerankingProperties;
import com.aislam.rag.domain.SourceChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordRerankerTest {

    private KeywordReranker reranker;

    @BeforeEach
    void setUp() {
        reranker = new KeywordReranker(RerankingProperties.forTests());
    }

    @Test
    void boostsChunksWithMatchingQuestionTerms() {
        List<SourceChunk> candidates = List.of(
                chunk("1", "Genel bilgi metni.", 0.85),
                chunk("2", "Namazın farzları ayrıntılı anlatılır.", 0.70)
        );

        List<SourceChunk> reranked = reranker.rerank("Namazın farzları nelerdir?", candidates, 2);

        assertEquals("2", reranked.getFirst().id());
        assertTrue(reranked.getFirst().score() > reranked.get(1).score());
        assertTrue(reranked.getFirst().keywordScore() > 0.0 || reranked.getFirst().phraseScore() > 0.0);
    }

    @Test
    void boostsChunksWithExactQuestionPhrases() {
        List<SourceChunk> candidates = List.of(
                chunk("1", "Abdest almak için su kullanılır.", 0.92),
                chunk("2", "Abdesti bozan haller şunlardır.", 0.75)
        );

        List<SourceChunk> reranked = reranker.rerank("Abdesti bozan durumlar nelerdir?", candidates, 2);

        assertEquals("2", reranked.getFirst().id());
        assertEquals(0.75, reranked.getFirst().vectorScore());
        assertTrue(reranked.getFirst().phraseScore() > 0.0);
        assertEquals(
                reranked.getFirst().vectorScore()
                        + reranked.getFirst().keywordScore()
                        + reranked.getFirst().phraseScore()
                        + reranked.getFirst().sectionTitleBoost()
                        - reranked.getFirst().qualityPenalty(),
                reranked.getFirst().score()
        );
    }

    @Test
    void boostsSectionTitleMatches() {
        List<SourceChunk> candidates = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc", "Title", "Source", "Abdesti Bozan", "Genel metin.", 1, 0, 0.70
                ),
                chunk("2", "Namaz ile ilgili bilgi.", 0.80)
        );

        List<SourceChunk> reranked = reranker.rerank("Abdesti bozan durumlar nelerdir?", candidates, 2);

        assertEquals("1", reranked.getFirst().id());
        assertTrue(reranked.getFirst().sectionTitleBoost() > 0.0);
    }

    @Test
    void penalizesTableOfContentsChunks() {
        List<SourceChunk> candidates = List.of(
                chunk("1", """
                        İÇİNDEKİLER
                        Abdest .............. 12
                        Namaz ............... 45
                        Oruç ................ 78
                        """, 0.92),
                chunk("2", "Abdest alma adabı anlatılır.", 0.75)
        );

        List<SourceChunk> reranked = reranker.rerank("Abdest nasıl alınır?", candidates, 2);

        assertEquals("2", reranked.getFirst().id());
        assertTrue(reranked.stream().filter(c -> c.id().equals("1")).findFirst().orElseThrow().qualityPenalty() > 0.0);
    }

    @Test
    void returnsTopKAfterReranking() {
        List<SourceChunk> candidates = List.of(
                chunk("1", "Birinci", 0.50),
                chunk("2", "İkinci namaz", 0.49),
                chunk("3", "Üçüncü", 0.48),
                chunk("4", "Dördüncü namaz", 0.47)
        );

        List<SourceChunk> reranked = reranker.rerank("namaz", candidates, 2);

        assertEquals(2, reranked.size());
    }

    private SourceChunk chunk(String id, String content, double vectorScore) {
        return SourceChunk.fromVectorSearch(id, "doc", "Title", "Source", content, 1, 0, vectorScore);
    }
}
