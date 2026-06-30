package com.aislam.rag.service;

import com.aislam.rag.client.EmbeddingClient;
import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.domain.PdfPageText;
import com.aislam.rag.domain.QdrantPoint;
import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.domain.TextChunk;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.SectionTitleIndexStats;
import com.aislam.rag.dto.UploadDocumentRequest;
import com.aislam.rag.exception.RagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentIndexServiceImpl implements DocumentIndexService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexServiceImpl.class);

    private final ChunkingService chunkingService;
    private final PdfTextExtractor pdfTextExtractor;
    private final PdfTextCleaner pdfTextCleaner;
    private final EmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;

    public DocumentIndexServiceImpl(
            ChunkingService chunkingService,
            PdfTextExtractor pdfTextExtractor,
            PdfTextCleaner pdfTextCleaner,
            EmbeddingClient embeddingClient,
            QdrantClient qdrantClient
    ) {
        this.chunkingService = chunkingService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.pdfTextCleaner = pdfTextCleaner;
        this.embeddingClient = embeddingClient;
        this.qdrantClient = qdrantClient;
    }

    @Override
    public IndexDocumentResponse index(RagDocument document) {
        String documentId = document.id() != null ? document.id() : UUID.randomUUID().toString();
        List<TextChunk> chunks = chunkingService.chunk(document);
        return indexChunks(document.withId(documentId), chunks);
    }

    @Override
    public IndexDocumentResponse index(UploadDocumentRequest request) {
        RagDocument document = RagDocument.of(
                request.title(),
                request.source(),
                request.content(),
                request.resolvedCategory()
        );
        return index(document);
    }

    @Override
    public IndexDocumentResponse indexPdf(RagDocument document, byte[] pdfBytes) {
        String documentId = document.id() != null ? document.id() : UUID.randomUUID().toString();
        List<PdfPageText> pages = pdfTextExtractor.extractPages(pdfBytes);
        Set<String> repeatedHeaders = pdfTextCleaner.detectRepeatedHeaders(
                pages.stream().map(PdfPageText::text).toList()
        );

        List<PdfPageText> cleanedPages = pages.stream()
                .map(page -> new PdfPageText(
                        page.pageNumber(),
                        pdfTextCleaner.clean(page.text(), repeatedHeaders)
                ))
                .filter(page -> page.text() != null && !page.text().isBlank())
                .toList();

        if (cleanedPages.isEmpty()) {
            throw new RagException("No text could be extracted from PDF");
        }

        List<TextChunk> chunks = chunkingService.chunkPages(cleanedPages);
        SectionTitleIndexStats sectionTitleStats = SectionTitleIndexStats.fromChunks(chunks);
        log.info(
                "PDF section title extraction summary documentId={} totalChunks={} chunksWithSectionTitle={} chunksWithoutSectionTitle={}",
                documentId,
                sectionTitleStats.totalChunks(),
                sectionTitleStats.chunksWithSectionTitle(),
                sectionTitleStats.chunksWithoutSectionTitle()
        );
        return indexChunks(document.withId(documentId), chunks, sectionTitleStats);
    }

    private IndexDocumentResponse indexChunks(RagDocument document, List<TextChunk> chunks) {
        return indexChunks(document, chunks, null);
    }

    private IndexDocumentResponse indexChunks(
            RagDocument document,
            List<TextChunk> chunks,
            SectionTitleIndexStats sectionTitleStats
    ) {
        String documentId = document.id();

        if (chunks.isEmpty()) {
            return IndexDocumentResponse.empty(documentId);
        }

        List<QdrantPoint> points = new ArrayList<>();

        for (TextChunk chunk : chunks) {
            float[] vector = embeddingClient.createEmbedding(chunk.content());

            Map<String, Object> payload = new HashMap<>();
            payload.put("documentId", documentId);
            payload.put("title", document.title());
            payload.put("source", document.source());
            payload.put("content", chunk.content());
            payload.put("category", document.category() != null ? document.category() : "general");
            payload.put("chunkIndex", chunk.chunkIndex());
            payload.put("sectionTitle", chunk.sectionTitle() != null ? chunk.sectionTitle() : "");
            if (chunk.pageNumber() != null) {
                payload.put("pageNumber", chunk.pageNumber());
            }

            points.add(new QdrantPoint(
                    UUID.randomUUID().toString(),
                    toFloatList(vector),
                    payload
            ));
        }

        qdrantClient.upsert(points);
        return IndexDocumentResponse.indexed(documentId, points.size(), sectionTitleStats);
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
