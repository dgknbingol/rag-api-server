package com.aislam.rag.client;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.LmStudioProperties;
import com.aislam.rag.config.RagConcurrencyLimiter;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class EmbeddingClientImpl implements EmbeddingClient {

    private final WebClient webClient;
    private final String embeddingModel;
    private final RagConcurrencyLimiter concurrencyLimiter;

    public EmbeddingClientImpl(
            @Qualifier("lmStudioWebClient") WebClient webClient,
            LmStudioProperties properties,
            RagConcurrencyLimiter concurrencyLimiter
    ) {
        this.webClient = webClient;
        this.embeddingModel = properties.embeddingModel();
        this.concurrencyLimiter = concurrencyLimiter;
    }

    @Override
    public float[] createEmbedding(String text) {
        return concurrencyLimiter.withEmbeddingPermit(() -> doCreateEmbedding(text));
    }

    private float[] doCreateEmbedding(String text) {
        Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", text
        );

        JsonNode response = webClient.post()
                .uri("/v1/embeddings")
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("data") || !response.get("data").isArray() || response.get("data").isEmpty()) {
            throw new RagException("Embedding response is empty or invalid");
        }

        JsonNode embeddingNode = response.get("data").get(0).get("embedding");
        if (embeddingNode == null || !embeddingNode.isArray()) {
            throw new RagException("Embedding vector not found in response");
        }

        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }
}
