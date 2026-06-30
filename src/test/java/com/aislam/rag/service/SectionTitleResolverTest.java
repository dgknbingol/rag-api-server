package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.domain.SourceChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectionTitleResolverTest {

    private QdrantClient qdrantClient;
    private SectionTitleResolver resolver;

    @BeforeEach
    void setUp() {
        qdrantClient = mock(QdrantClient.class);
        resolver = new SectionTitleResolver(qdrantClient);
    }

    @Test
    void inheritsSectionTitleFromPreviousChunkInBatch() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc-1", "Title", "Source", "ABDESTİ BOZAN HALLER",
                        "1. Kanal suyu.", 12, 0, 0.90
                ),
                SourceChunk.fromVectorSearch(
                        "2", "doc-1", "Title", "Source", null,
                        "2. Yüz yıkanır.", 12, 1, 0.85
                )
        );

        List<SourceChunk> resolved = resolver.resolve(chunks);

        assertEquals("ABDESTİ BOZAN HALLER", resolved.get(1).sectionTitle());
    }

    @Test
    void fetchesSectionTitleFromPreviousQdrantChunkWhenMissingInBatch() {
        List<SourceChunk> chunks = List.of(
                SourceChunk.fromVectorSearch(
                        "2", "doc-1", "Title", "Source", null,
                        "2. Yüz yıkanır.", 12, 2, 0.85
                )
        );

        when(qdrantClient.findByDocumentChunkKeys(any())).thenReturn(List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc-1", "Title", "Source", "ABDESTİ BOZAN HALLER",
                        "1. Kanal suyu.", 12, 1, 0.90
                )
        ));

        List<SourceChunk> resolved = resolver.resolve(chunks);

        assertEquals("ABDESTİ BOZAN HALLER", resolved.getFirst().sectionTitle());
    }
}
