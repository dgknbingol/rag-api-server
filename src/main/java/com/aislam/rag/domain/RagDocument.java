package com.aislam.rag.domain;

public record RagDocument(
        String id,
        String title,
        String source,
        String content,
        String category
) {
    public static RagDocument of(String title, String source, String content, String category) {
        return new RagDocument(null, title, source, content, category);
    }

    public RagDocument withId(String id) {
        return new RagDocument(id, title, source, content, category);
    }
}
