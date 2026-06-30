package com.aislam.rag.client;

public interface EmbeddingClient {

    float[] createEmbedding(String text);
}
