package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.domain.DocumentChunkKey;
import com.aislam.rag.domain.SourceChunk;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SectionTitleResolver {

    private static final int LOOKUP_WINDOW = 20;

    private final QdrantClient qdrantClient;

    public SectionTitleResolver(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    public List<SourceChunk> resolve(List<SourceChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Map<DocumentChunkKey, String> knownTitles = new HashMap<>();
        for (SourceChunk chunk : chunks) {
            if (hasSectionTitle(chunk)) {
                knownTitles.put(toKey(chunk), chunk.sectionTitle());
            }
        }

        knownTitles.putAll(fetchMissingPreviousTitles(chunks, knownTitles));

        return chunks.stream()
                .map(chunk -> enrich(chunk, knownTitles))
                .toList();
    }

    private SourceChunk enrich(SourceChunk chunk, Map<DocumentChunkKey, String> knownTitles) {
        if (hasSectionTitle(chunk)) {
            return chunk;
        }

        String nearbyTitle = findNearbyTitle(chunk, knownTitles);
        return nearbyTitle != null ? chunk.withSectionTitle(nearbyTitle) : chunk;
    }

    private Map<DocumentChunkKey, String> fetchMissingPreviousTitles(
            List<SourceChunk> chunks,
            Map<DocumentChunkKey, String> knownTitles
    ) {
        Set<DocumentChunkKey> keysToFetch = new HashSet<>();
        for (SourceChunk chunk : chunks) {
            if (hasSectionTitle(chunk) || chunk.documentId() == null || chunk.chunkIndex() == null) {
                continue;
            }
            for (int index = chunk.chunkIndex() - 1; index >= Math.max(0, chunk.chunkIndex() - LOOKUP_WINDOW); index--) {
                DocumentChunkKey key = new DocumentChunkKey(chunk.documentId(), index);
                if (!knownTitles.containsKey(key)) {
                    keysToFetch.add(key);
                }
            }
        }

        if (keysToFetch.isEmpty()) {
            return Map.of();
        }

        Map<DocumentChunkKey, String> fetchedTitles = new HashMap<>();
        for (SourceChunk chunk : qdrantClient.findByDocumentChunkKeys(keysToFetch)) {
            if (hasSectionTitle(chunk)) {
                fetchedTitles.put(toKey(chunk), chunk.sectionTitle());
            }
        }
        return fetchedTitles;
    }

    private String findNearbyTitle(SourceChunk chunk, Map<DocumentChunkKey, String> titlesByKey) {
        if (chunk.documentId() == null || chunk.chunkIndex() == null) {
            return null;
        }

        for (int index = chunk.chunkIndex() - 1; index >= Math.max(0, chunk.chunkIndex() - LOOKUP_WINDOW); index--) {
            String title = titlesByKey.get(new DocumentChunkKey(chunk.documentId(), index));
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return null;
    }

    private boolean hasSectionTitle(SourceChunk chunk) {
        return chunk.sectionTitle() != null && !chunk.sectionTitle().isBlank();
    }

    private DocumentChunkKey toKey(SourceChunk chunk) {
        return new DocumentChunkKey(chunk.documentId(), chunk.chunkIndex());
    }
}
