package com.aislam.rag.client;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.QdrantProperties;
import com.aislam.rag.domain.DocumentChunkKey;
import com.aislam.rag.domain.QdrantPoint;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.util.Utf8Strings;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QdrantClientImpl implements QdrantClient {

    private final WebClient webClient;
    private final String collectionName;
    private final int vectorSize;

    public QdrantClientImpl(
            @Qualifier("qdrantWebClient") WebClient webClient,
            QdrantProperties properties
    ) {
        this.webClient = webClient;
        this.collectionName = properties.collectionName();
        this.vectorSize = properties.vectorSize();
    }

    @Override
    public void ensureCollectionExists() {
        try {
            webClient.get()
                    .uri("/collections/{collection}", collectionName)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException.NotFound notFound) {
            createCollection();
        }
    }

    private void createCollection() {
        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", vectorSize,
                        "distance", "Cosine"
                )
        );

        webClient.put()
                .uri("/collections/{collection}", collectionName)
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(byte[].class)
                                .map(Utf8Strings::fromBytes)
                                .map(msg -> new RagException("Failed to create Qdrant collection: " + msg))
                )
                .bodyToMono(Void.class)
                .block();
    }

    @Override
    public List<SourceChunk> search(float[] vector, int limit) {
        Map<String, Object> body = Map.of(
                "vector", toFloatList(vector),
                "limit", limit,
                "with_payload", true
        );

        JsonNode response = webClient.post()
                .uri("/collections/{collection}/points/search", collectionName)
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("result")) {
            throw new RagException("Qdrant search response is invalid");
        }

        List<SourceChunk> chunks = new ArrayList<>();
        for (JsonNode point : response.get("result")) {
            chunks.add(mapPoint(point));
        }
        return chunks;
    }

    @Override
    public List<SourceChunk> findByDocumentChunkKeys(Collection<DocumentChunkKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> should = new ArrayList<>();
        for (DocumentChunkKey key : keys) {
            should.add(Map.of(
                    "must", List.of(
                            Map.of("key", "documentId", "match", Map.of("value", key.documentId())),
                            Map.of("key", "chunkIndex", "match", Map.of("value", key.chunkIndex()))
                    )
            ));
        }

        Map<String, Object> body = Map.of(
                "filter", Map.of("should", should),
                "limit", keys.size(),
                "with_payload", true,
                "with_vector", false
        );

        JsonNode response = webClient.post()
                .uri("/collections/{collection}/points/scroll", collectionName)
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("result") || !response.get("result").has("points")) {
            throw new RagException("Qdrant scroll response is invalid");
        }

        List<SourceChunk> chunks = new ArrayList<>();
        for (JsonNode point : response.get("result").get("points")) {
            chunks.add(mapScrollPoint(point));
        }
        return chunks;
    }

    @Override
    public void upsert(List<QdrantPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        List<Map<String, Object>> pointMaps = new ArrayList<>();
        for (QdrantPoint point : points) {
            Map<String, Object> pointMap = new HashMap<>();
            pointMap.put("id", point.id());
            pointMap.put("vector", point.vector());
            pointMap.put("payload", point.payload());
            pointMaps.add(pointMap);
        }

        Map<String, Object> body = Map.of(
                "points", pointMaps,
                "wait", true
        );

        webClient.put()
                .uri("/collections/{collection}/points?wait=true", collectionName)
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    private SourceChunk mapPoint(JsonNode point) {
        String id = point.has("id") ? point.get("id").asText() : null;
        double score = point.has("score") ? point.get("score").asDouble() : 0.0;
        JsonNode payload = point.get("payload");

        return SourceChunk.fromVectorSearch(
                id,
                firstNonBlank(
                        textFromPayload(payload, "documentId"),
                        textFromPayload(payload, "document_id")
                ),
                textFromPayload(payload, "title"),
                textFromPayload(payload, "source"),
                textFromPayload(payload, "sectionTitle"),
                firstNonBlank(
                        textFromPayload(payload, "content"),
                        textFromPayload(payload, "text")
                ),
                integerFromPayload(payload, "pageNumber"),
                integerFromPayload(payload, "chunkIndex"),
                score
        );
    }

    private SourceChunk mapScrollPoint(JsonNode point) {
        String id = point.has("id") ? point.get("id").asText() : null;
        JsonNode payload = point.get("payload");

        return SourceChunk.fromVectorSearch(
                id,
                firstNonBlank(
                        textFromPayload(payload, "documentId"),
                        textFromPayload(payload, "document_id")
                ),
                textFromPayload(payload, "title"),
                textFromPayload(payload, "source"),
                textFromPayload(payload, "sectionTitle"),
                firstNonBlank(
                        textFromPayload(payload, "content"),
                        textFromPayload(payload, "text")
                ),
                integerFromPayload(payload, "pageNumber"),
                integerFromPayload(payload, "chunkIndex"),
                0.0
        );
    }

    private Integer integerFromPayload(JsonNode payload, String field) {
        if (payload == null || !payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        JsonNode node = payload.get(field);
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String textFromPayload(JsonNode payload, String field) {
        if (payload == null || !payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).asText();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
