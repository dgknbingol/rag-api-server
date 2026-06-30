package com.aislam.rag.service;

import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.PdfUploadResponse;
import com.aislam.rag.dto.SectionTitleIndexStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfIngestionServiceImplTest {

    private DocumentIndexService documentIndexService;
    private PdfIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        documentIndexService = mock(DocumentIndexService.class);
        service = new PdfIngestionServiceImpl(documentIndexService);
    }

    @Test
    void ingestsPdfAndReturnsIndexedResponse() {
        byte[] pdfBytes = "%PDF-1.4".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ilmihal.pdf",
                "application/pdf",
                pdfBytes
        );

        when(documentIndexService.indexPdf(any(RagDocument.class), eq(pdfBytes)))
                .thenReturn(IndexDocumentResponse.indexed(
                        "doc-123",
                        5,
                        new SectionTitleIndexStats(5, 3, 2)
                ));

        PdfUploadResponse response = service.ingest(file, "İslam şartları", "Ilmihal", "temel_bilgiler");

        assertEquals("doc-123", response.documentId());
        assertEquals("İslam şartları", response.title());
        assertEquals(5, response.chunkCount());
        assertEquals("INDEXED", response.status());
        assertEquals(5, response.sectionTitleStats().totalChunks());
        assertEquals(3, response.sectionTitleStats().chunksWithSectionTitle());
        assertEquals(2, response.sectionTitleStats().chunksWithoutSectionTitle());
        verify(documentIndexService).indexPdf(any(RagDocument.class), eq(pdfBytes));
    }
}
