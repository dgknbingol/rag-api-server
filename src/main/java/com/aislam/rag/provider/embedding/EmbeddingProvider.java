package com.aislam.rag.provider.embedding;

public interface EmbeddingProvider {

    String id();

    float[] embed(String text);
}
