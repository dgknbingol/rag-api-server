package com.aislam.rag.provider.llm;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.DeepSeekProperties;
import com.aislam.rag.config.RagConcurrencyLimiter;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.provider.ProviderIds;
import com.aislam.rag.util.Utf8Strings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DeepSeekLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekLlmProvider.class);
    private static final String ERROR_CODE = "DEEPSEEK_ERROR";

    private final WebClient webClient;
    private final DeepSeekProperties properties;
    private final RagConcurrencyLimiter concurrencyLimiter;
    private final ObjectMapper objectMapper;

    public DeepSeekLlmProvider(
            @Qualifier("deepSeekWebClient") WebClient webClient,
            DeepSeekProperties properties,
            RagConcurrencyLimiter concurrencyLimiter,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.properties = properties;
        this.concurrencyLimiter = concurrencyLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return ProviderIds.LLM_DEEPSEEK;
    }

    @Override
    public String chat(String question) {
        return chatMessages(List.of(
                Map.of("role", "system", "content", properties.systemPrompt()),
                Map.of("role", "user", "content", question)
        ));
    }

    @Override
    public String complete(String prompt) {
        return concurrencyLimiter.withChatPermit(() -> request(
                List.of(Map.of("role", "user", "content", prompt)),
                false,
                null
        ));
    }

    @Override
    public String chatMessages(List<Map<String, String>> messages) {
        return concurrencyLimiter.withChatPermit(() -> request(messages, false, null));
    }

    @Override
    public void streamChat(List<Map<String, String>> messages, Consumer<String> onDelta) {
        concurrencyLimiter.withChatPermit(() -> {
            request(messages, true, onDelta);
            return null;
        });
    }

    private String request(
            List<Map<String, String>> messages,
            boolean stream,
            Consumer<String> onDelta
    ) {
        ensureApiKeyConfigured();

        int inputLength = messages.stream()
                .mapToInt(item -> item.getOrDefault("content", "").length())
                .sum();

        log.info(
                "DeepSeek request model={} inputLength={} maxTokens={} temperature={} stream={}",
                properties.model(),
                inputLength,
                properties.maxTokens(),
                properties.temperature(),
                stream
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("messages", messages);
        body.put("temperature", properties.temperature());
        body.put("max_tokens", properties.maxTokens());
        body.put("stream", stream);

        Duration readTimeout = properties.readTimeout() == null
                ? Duration.ofSeconds(120)
                : properties.readTimeout();

        try {
            if (stream) {
                return streamRequest(body, onDelta, readTimeout);
            }

            byte[] responseBytes = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(byte[].class)
                                    .defaultIfEmpty(new byte[0])
                                    .flatMap(bytes -> {
                                        String errorBody = Utf8Strings.fromBytes(bytes);
                                        log.error(
                                                "DeepSeek error status={} body={}",
                                                clientResponse.statusCode(),
                                                errorBody
                                        );
                                        return Mono.error(new RagException(
                                                buildErrorMessage(clientResponse.statusCode().value(), errorBody),
                                                ERROR_CODE
                                        ));
                                    })
                    )
                    .bodyToMono(byte[].class)
                    .timeout(readTimeout)
                    .block(readTimeout);

            return parseSuccessResponse(responseBytes);
        } catch (RagException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            String errorBody = Utf8Strings.fromBytes(ex.getResponseBodyAsByteArray());
            log.error("DeepSeek error status={} body={}", ex.getStatusCode(), errorBody);
            throw new RagException(
                    buildErrorMessage(ex.getStatusCode().value(), errorBody),
                    ERROR_CODE,
                    ex
            );
        } catch (WebClientRequestException ex) {
            log.error("DeepSeek connection failed: {}", ex.getMessage(), ex);
            throw new RagException("DeepSeek connection failed: " + ex.getMessage(), ERROR_CODE, ex);
        } catch (Exception ex) {
            log.error("DeepSeek request failed: {}", ex.getMessage(), ex);
            throw new RagException("DeepSeek request failed: " + ex.getMessage(), ERROR_CODE, ex);
        }
    }

    private String streamRequest(
            Map<String, Object> body,
            Consumer<String> onDelta,
            Duration readTimeout
    ) {
        StringBuilder full = new StringBuilder();

        Flux<String> lines = webClient.post()
                .uri("/v1/chat/completions")
                .contentType(ApiMediaTypes.APPLICATION_JSON_UTF8)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(byte[].class)
                                .defaultIfEmpty(new byte[0])
                                .flatMap(bytes -> Mono.error(new RagException(
                                        buildErrorMessage(
                                                clientResponse.statusCode().value(),
                                                Utf8Strings.fromBytes(bytes)
                                        ),
                                        ERROR_CODE
                                )))
                )
                .bodyToFlux(String.class)
                .timeout(readTimeout);

        lines.doOnNext(chunk -> {
            for (String line : splitSseLines(chunk)) {
                if (line.isBlank() || "data: [DONE]".equals(line)) {
                    continue;
                }
                String data = line.startsWith("data:") ? line.substring(5).trim() : line.trim();
                if (data.isBlank() || "[DONE]".equals(data)) {
                    continue;
                }
                String delta = parseStreamDelta(data);
                if (delta != null && !delta.isEmpty()) {
                    full.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
            }
        }).blockLast(readTimeout);

        String answer = full.toString().trim();
        if (answer.isBlank()) {
            throw new RagException("DeepSeek stream content is empty", ERROR_CODE);
        }
        return answer;
    }

    private List<String> splitSseLines(String chunk) {
        String[] parts = chunk.split("\\r?\\n");
        List<String> lines = new ArrayList<>(parts.length);
        for (String part : parts) {
            lines.add(part.trim());
        }
        return lines;
    }

    private String parseStreamDelta(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode choices = node.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode delta = choices.get(0).path("delta").path("content");
            if (delta.isMissingNode() || delta.isNull()) {
                // some gateways send message.content on non-stream chunks
                delta = choices.get(0).path("message").path("content");
            }
            return delta.isMissingNode() || delta.isNull() ? null : delta.asText();
        } catch (Exception ex) {
            log.debug("Skipping non-JSON stream chunk: {}", json);
            return null;
        }
    }

    private void ensureApiKeyConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new RagException(
                    "DeepSeek API key is not configured. Set deepseek.api-key in application-local.yml",
                    ERROR_CODE
            );
        }
    }

    private String parseSuccessResponse(byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length == 0) {
            throw new RagException("DeepSeek returned an empty response", ERROR_CODE);
        }

        String json = new String(responseBytes, StandardCharsets.UTF_8);

        JsonNode response;
        try {
            response = objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new RagException("DeepSeek response JSON is invalid", ERROR_CODE, ex);
        }

        if (response.has("error")) {
            String errorMessage = response.path("error").path("message").asText("Unknown DeepSeek error");
            throw new RagException("DeepSeek error: " + errorMessage, ERROR_CODE);
        }

        if (!response.has("choices")
                || !response.get("choices").isArray()
                || response.get("choices").isEmpty()) {
            throw new RagException("DeepSeek response has no choices", ERROR_CODE);
        }

        JsonNode contentNode = response.get("choices").get(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new RagException("DeepSeek response content is empty", ERROR_CODE);
        }

        return contentNode.asText().trim();
    }

    private String buildErrorMessage(int statusCode, String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return "DeepSeek returned HTTP " + statusCode;
        }
        String compactBody = errorBody.length() > 500 ? errorBody.substring(0, 497) + "..." : errorBody;
        return "DeepSeek returned HTTP " + statusCode + ": " + compactBody;
    }
}
