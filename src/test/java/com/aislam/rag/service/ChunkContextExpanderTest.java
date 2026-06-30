package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.SourceChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkContextExpanderTest {

    private QdrantClient qdrantClient;
    private ChunkContextExpander expander;

    @BeforeEach
    void setUp() {
        qdrantClient = mock(QdrantClient.class);
        expander = new ChunkContextExpander(qdrantClient, RagProperties.forTests());
    }

    @Test
    void fetchesNeighborChunksAndSortsByPageAndIndex() {
        SourceChunk selected = SourceChunk.fromVectorSearch(
                "3", "doc-1", "Abdest", "İlmihal", "3. Kanal suyu.", 12, 2, 0.91
        );
        when(qdrantClient.findByDocumentChunkKeys(any())).thenReturn(List.of(
                SourceChunk.fromVectorSearch("2", "doc-1", "Abdest", "İlmihal", "2. Yüz yıkanır.", 12, 1, 0.0),
                SourceChunk.fromVectorSearch("4", "doc-1", "Abdest", "İlmihal", "4. Baş mesh edilir.", 12, 3, 0.0)
        ));

        List<SourceChunk> expanded = expander.expand(List.of(selected));

        assertEquals(3, expanded.size());
        assertEquals(1, expanded.get(0).chunkIndex());
        assertEquals(2, expanded.get(1).chunkIndex());
        assertEquals(3, expanded.get(2).chunkIndex());
        verify(qdrantClient).findByDocumentChunkKeys(any());
    }

    @Test
    void deduplicatesOverlappingNeighborRequests() {
        SourceChunk first = SourceChunk.fromVectorSearch(
                "1", "doc-1", "Abdest", "İlmihal", "Orta parça.", 12, 2, 0.90
        );
        SourceChunk second = SourceChunk.fromVectorSearch(
                "2", "doc-1", "Abdest", "İlmihal", "Sonraki parça.", 12, 3, 0.88
        );
        when(qdrantClient.findByDocumentChunkKeys(any())).thenReturn(List.of(
                SourceChunk.fromVectorSearch("0", "doc-1", "Abdest", "İlmihal", "İlk parça.", 12, 1, 0.0),
                SourceChunk.fromVectorSearch("5", "doc-1", "Abdest", "İlmihal", "Son parça.", 12, 4, 0.0)
        ));

        List<SourceChunk> expanded = expander.expand(List.of(first, second));

        assertEquals(4, expanded.size());
        assertEquals(1, expanded.get(0).chunkIndex());
        assertEquals(4, expanded.get(3).chunkIndex());
    }

    @Test
    void limitsExpandedContextToConfiguredMaxChars() {
        String longContent = "a".repeat(4000);
        SourceChunk selected = SourceChunk.fromVectorSearch(
                "1", "doc-1", "Abdest", "İlmihal", longContent, 12, 0, 0.90
        );
        when(qdrantClient.findByDocumentChunkKeys(any())).thenReturn(List.of(
                SourceChunk.fromVectorSearch("2", "doc-1", "Abdest", "İlmihal", longContent, 12, 1, 0.0)
        ));

        List<SourceChunk> expanded = expander.expand(List.of(selected));

        int totalChars = expanded.stream()
                .mapToInt(chunk -> chunk.content() != null ? chunk.content().length() : 0)
                .sum();
        assertTrue(totalChars <= 8000);
    }
}
