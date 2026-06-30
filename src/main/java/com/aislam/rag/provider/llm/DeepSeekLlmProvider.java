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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return concurrencyLimiter.withChatPermit(() -> request(
                List.of(
                        Map.of("role", "system", "content", properties.systemPrompt()),
                        Map.of("role", "user", "content", question)
                ),
                question.length()
        ));
    }

    @Override
    public String complete(String prompt) {
        return concurrencyLimiter.withChatPermit(() -> request(
                List.of(Map.of("role", "user", "content", prompt)),
                prompt.length()
        ));
    }

    private String request(List<Map<String, String>> messages, int inputLength) {
        ensureApiKeyConfigured();

        log.info(
                "DeepSeek request model={} inputLength={} maxTokens={} temperature={}",
                properties.model(),
                inputLength,
                properties.maxTokens(),
                properties.temperature()
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.model());
        body.put("messages", messages);
        body.put("temperature", properties.temperature());
        body.put("max_tokens", properties.maxTokens());
        body.put("stream", false);

        try {
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
                    .block();

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
