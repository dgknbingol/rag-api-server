package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.domain.TextChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingServiceImplTest {

    private ChunkingServiceImpl chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl(
                new ParagraphSplitter(),
                new ParagraphChunkAggregator(),
                new SectionParagraphExtractor(new SectionTitleDetector()),
                RagProperties.forTests()
        );
    }

    @Test
    void chunk_shortText_returnsSingleChunkWithNullPage() {
        List<TextChunk> chunks = chunkingService.chunk("Kısa metin");

        assertEquals(1, chunks.size());
        assertNull(chunks.getFirst().pageNumber());
        assertEquals(0, chunks.getFirst().chunkIndex());
    }

    @Test
    void chunk_keepsParagraphsTogether() {
        String text = "Namazin Farzlari\n\nparagraph1\n\nparagraph2";
        List<TextChunk> chunks = chunkingService.chunk(text);

        assertEquals(1, chunks.size());
        assertTrue(chunks.getFirst().content().contains("Namazin Farzlari"));
        assertTrue(chunks.getFirst().content().contains("paragraph1"));
    }

    @Test
    void chunk_document_usesContent() {
        RagDocument doc = RagDocument.of("Başlık", "Kaynak", "İçerik metni", "temel_bilgiler");
        List<TextChunk> chunks = chunkingService.chunk(doc);

        assertEquals(1, chunks.size());
        assertEquals("İçerik metni", chunks.getFirst().content());
    }

    @Test
    void chunkPages_assignsGlobalChunkIndexPageNumberAndSectionTitle() {
        List<TextChunk> chunks = chunkingService.chunkPages(List.of(
                new com.aislam.rag.domain.PdfPageText(1, """
                        ABDESTİ BOZAN HALLER
                        Birinci madde metni.
                        """),
                new com.aislam.rag.domain.PdfPageText(2, "Sayfa iki paragraf")
        ));

        assertEquals(2, chunks.size());
        assertEquals(0, chunks.get(0).chunkIndex());
        assertEquals(1, chunks.get(0).pageNumber());
        assertEquals("ABDESTİ BOZAN HALLER", chunks.get(0).sectionTitle());
        assertEquals(1, chunks.get(1).chunkIndex());
        assertEquals(2, chunks.get(1).pageNumber());
        assertEquals("ABDESTİ BOZAN HALLER", chunks.get(1).sectionTitle());
    }
}
