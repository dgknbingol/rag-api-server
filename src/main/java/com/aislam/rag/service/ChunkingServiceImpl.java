package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.PdfPageText;
import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.domain.TextChunk;
import com.aislam.rag.dto.SectionTitleIndexStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingServiceImpl.class);

    private final ParagraphSplitter paragraphSplitter;
    private final ParagraphChunkAggregator paragraphChunkAggregator;
    private final SectionParagraphExtractor sectionParagraphExtractor;
    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingServiceImpl(
            ParagraphSplitter paragraphSplitter,
            ParagraphChunkAggregator paragraphChunkAggregator,
            SectionParagraphExtractor sectionParagraphExtractor,
            RagProperties properties
    ) {
        this.paragraphSplitter = paragraphSplitter;
        this.paragraphChunkAggregator = paragraphChunkAggregator;
        this.sectionParagraphExtractor = sectionParagraphExtractor;
        this.chunkSize = properties.chunkSize();
        this.chunkOverlap = properties.chunkOverlap();
    }

    @Override
    public List<TextChunk> chunk(String text) {
        List<String> paragraphs = paragraphSplitter.split(text);
        List<TextChunk> chunks = assignChunkIndexes(
                paragraphChunkAggregator.aggregate(paragraphs, chunkSize, chunkSize, chunkOverlap),
                null
        );
        logChunkStats(0, paragraphs.size(), chunks);
        return chunks;
    }

    @Override
    public List<TextChunk> chunk(RagDocument document) {
        return chunk(document.content());
    }

    @Override
    public List<TextChunk> chunkPages(List<PdfPageText> pages) {
        if (pages == null || pages.isEmpty()) {
            logChunkStats(0, 0, List.of());
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int globalChunkIndex = 0;
        int totalParagraphs = 0;
        String lastSectionTitle = null;

        for (PdfPageText page : pages) {
            if (page.text() == null || page.text().isBlank()) {
                continue;
            }

            var extraction = sectionParagraphExtractor.extract(page.text(), lastSectionTitle);
            lastSectionTitle = extraction.lastSectionTitle();
            totalParagraphs += extraction.paragraphs().size();
            var aggregatedChunks = paragraphChunkAggregator.aggregateSectionParagraphs(
                    extraction.paragraphs(),
                    chunkSize,
                    chunkSize,
                    chunkOverlap
            );

            for (var aggregatedChunk : aggregatedChunks) {
                TextChunk chunk = new TextChunk(
                        aggregatedChunk.content(),
                        globalChunkIndex,
                        page.pageNumber(),
                        aggregatedChunk.sectionTitle()
                );
                log.info(
                        "Detected chunk section title pageNumber={} chunkIndex={} sectionTitle={}",
                        chunk.pageNumber(),
                        chunk.chunkIndex(),
                        formatSectionTitle(chunk.sectionTitle())
                );
                chunks.add(chunk);
                globalChunkIndex++;
            }
        }

        logChunkStats(pages.size(), totalParagraphs, chunks);
        return List.copyOf(chunks);
    }

    private static String formatSectionTitle(String sectionTitle) {
        if (sectionTitle == null || sectionTitle.isBlank()) {
            return "(none)";
        }
        return sectionTitle;
    }

    private static boolean hasSectionTitle(TextChunk chunk) {
        return chunk.sectionTitle() != null && !chunk.sectionTitle().isBlank();
    }

    private List<TextChunk> assignChunkIndexes(List<String> chunkTexts, Integer pageNumber) {
        List<TextChunk> chunks = new ArrayList<>(chunkTexts.size());
        for (int i = 0; i < chunkTexts.size(); i++) {
            chunks.add(new TextChunk(chunkTexts.get(i), i, pageNumber));
        }
        return List.copyOf(chunks);
    }

    private void logChunkStats(int totalPages, int totalParagraphs, List<TextChunk> chunks) {
        int totalChunks = chunks.size();
        double averageChunkSize = chunks.stream()
                .mapToInt(chunk -> chunk.content().length())
                .average()
                .orElse(0.0);

        SectionTitleIndexStats sectionTitleStats = SectionTitleIndexStats.fromChunks(chunks);

        log.info(
                "Chunking complete totalPages={} totalParagraphs={} totalChunks={} averageChunkSize={} "
                        + "chunksWithSectionTitle={} chunksWithoutSectionTitle={}",
                totalPages,
                totalParagraphs,
                totalChunks,
                Math.round(averageChunkSize),
                sectionTitleStats.chunksWithSectionTitle(),
                sectionTitleStats.chunksWithoutSectionTitle()
        );
    }
}
