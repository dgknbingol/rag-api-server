package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.DocumentChunkKey;
import com.aislam.rag.domain.SourceChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ChunkContextExpander {

    private static final String TRUNCATION_SUFFIX = "...";

    private final QdrantClient qdrantClient;
    private final int neighborWindow;
    private final int maxContextChars;

    public ChunkContextExpander(QdrantClient qdrantClient, RagProperties properties) {
        this.qdrantClient = qdrantClient;
        this.neighborWindow = properties.neighborWindow();
        this.maxContextChars = properties.maxContextChars();
    }

    public List<SourceChunk> expand(List<SourceChunk> selectedChunks) {
        if (selectedChunks == null || selectedChunks.isEmpty()) {
            return List.of();
        }

        Map<DocumentChunkKey, SourceChunk> indexedChunks = new HashMap<>();
        List<SourceChunk> unindexedChunks = new ArrayList<>();
        Set<DocumentChunkKey> keysToResolve = new HashSet<>();

        for (SourceChunk chunk : selectedChunks) {
            if (chunk.documentId() == null || chunk.documentId().isBlank() || chunk.chunkIndex() == null) {
                unindexedChunks.add(chunk);
                continue;
            }

            DocumentChunkKey selectedKey = new DocumentChunkKey(chunk.documentId(), chunk.chunkIndex());
            indexedChunks.putIfAbsent(selectedKey, chunk);

            for (int offset = -neighborWindow; offset <= neighborWindow; offset++) {
                int neighborIndex = chunk.chunkIndex() + offset;
                if (neighborIndex < 0) {
                    continue;
                }
                keysToResolve.add(new DocumentChunkKey(chunk.documentId(), neighborIndex));
            }
        }

        Set<DocumentChunkKey> missingKeys = new HashSet<>();
        for (DocumentChunkKey key : keysToResolve) {
            if (!indexedChunks.containsKey(key)) {
                missingKeys.add(key);
            }
        }

        if (!missingKeys.isEmpty()) {
            for (SourceChunk neighbor : qdrantClient.findByDocumentChunkKeys(missingKeys)) {
                if (neighbor.documentId() == null || neighbor.chunkIndex() == null) {
                    continue;
                }
                indexedChunks.putIfAbsent(
                        new DocumentChunkKey(neighbor.documentId(), neighbor.chunkIndex()),
                        neighbor
                );
            }
        }

        List<SourceChunk> sortedIndexed = indexedChunks.values().stream()
                .sorted(compareByPageAndChunkIndex())
                .toList();

        List<SourceChunk> expanded = new ArrayList<>(sortedIndexed.size() + unindexedChunks.size());
        expanded.addAll(sortedIndexed);
        expanded.addAll(unindexedChunks);

        return applyCharLimit(expanded, maxContextChars);
    }

    private Comparator<SourceChunk> compareByPageAndChunkIndex() {
        return Comparator
                .comparing(SourceChunk::pageNumber, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SourceChunk::chunkIndex, Comparator.nullsLast(Integer::compareTo));
    }

    private List<SourceChunk> applyCharLimit(List<SourceChunk> chunks, int maxChars) {
        if (maxChars <= 0 || chunks.isEmpty()) {
            return List.of();
        }

        int usedChars = 0;
        List<SourceChunk> limited = new ArrayList<>();

        for (SourceChunk chunk : chunks) {
            if (usedChars >= maxChars) {
                break;
            }

            String content = chunk.content() != null ? chunk.content() : "";
            if (content.isEmpty()) {
                limited.add(chunk);
                continue;
            }

            int remaining = maxChars - usedChars;
            if (content.length() <= remaining) {
                limited.add(chunk);
                usedChars += content.length();
                continue;
            }

            if (remaining <= TRUNCATION_SUFFIX.length()) {
                break;
            }

            String truncated = content.substring(0, remaining - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
            limited.add(chunk.withContent(truncated));
            break;
        }

        return limited;
    }
}
