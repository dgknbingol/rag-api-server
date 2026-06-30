package com.aislam.rag.domain;

public record TextChunk(
        String content,
        int chunkIndex,
        Integer pageNumber,
        String sectionTitle
) {
    public TextChunk(String content, int chunkIndex, Integer pageNumber) {
        this(content, chunkIndex, pageNumber, null);
    }
}
