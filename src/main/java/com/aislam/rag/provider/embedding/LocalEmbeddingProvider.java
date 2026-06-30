package com.aislam.rag.provider.embedding;

import com.aislam.rag.client.EmbeddingClient;
import com.aislam.rag.provider.ProviderIds;
import org.springframework.stereotype.Component;

@Component
public class LocalEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingClient embeddingClient;

    public LocalEmbeddingProvider(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @Override
    public String id() {
        return ProviderIds.EMBEDDING_LOCAL;
    }

    @Override
    public float[] embed(String text) {
        return embeddingClient.createEmbedding(text);
    }
}
