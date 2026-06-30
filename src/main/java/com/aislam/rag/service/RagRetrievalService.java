package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.client.RerankerClient;
import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.dto.SearchResultDto;
import com.aislam.rag.provider.ProviderRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagRetrievalService {

    private final ProviderRegistry providerRegistry;
    private final QdrantClient qdrantClient;
    private final RerankerClient rerankerClient;
    private final SectionTitleResolver sectionTitleResolver;
    private final int initialCandidateCount;
    private final int finalSourceCount;
    private final int contentPreviewChars;

    public RagRetrievalService(
            ProviderRegistry providerRegistry,
            QdrantClient qdrantClient,
            RerankerClient rerankerClient,
            SectionTitleResolver sectionTitleResolver,
            RagProperties properties
    ) {
        this.providerRegistry = providerRegistry;
        this.qdrantClient = qdrantClient;
        this.rerankerClient = rerankerClient;
        this.sectionTitleResolver = sectionTitleResolver;
        this.initialCandidateCount = properties.initialCandidateCount();
        this.finalSourceCount = properties.finalSourceCount();
        this.contentPreviewChars = properties.contentPreviewChars();
    }

    public List<SourceChunk> retrieve(String question) {
        float[] embedding = providerRegistry.embedding().embed(question);
        List<SourceChunk> candidates = qdrantClient.search(embedding, initialCandidateCount);
        List<SourceChunk> enrichedCandidates = sectionTitleResolver.resolve(candidates);
        return rerankerClient.rerank(question, enrichedCandidates, finalSourceCount);
    }

    public List<SearchResultDto> retrieveSources(String question) {
        return toSearchResults(retrieve(question));
    }

    public List<SearchResultDto> toSearchResults(List<SourceChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new SearchResultDto(
                        chunk.title(),
                        chunk.source(),
                        chunk.sectionTitle(),
                        preview(chunk.content()),
                        chunk.pageNumber(),
                        chunk.chunkIndex(),
                        chunk.score(),
                        chunk.vectorScore(),
                        chunk.keywordScore(),
                        chunk.phraseScore(),
                        chunk.sectionTitleBoost(),
                        chunk.qualityPenalty()
                ))
                .toList();
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= contentPreviewChars) {
            return content;
        }
        return content.substring(0, contentPreviewChars).stripTrailing() + "...";
    }
}
