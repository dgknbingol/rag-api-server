package com.aislam.rag.client;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.LmStudioProperties;
import com.aislam.rag.config.RagConcurrencyLimiter;
import com.aislam.rag.debug.Utf8EncodingDebugLogger;
import com.aislam.rag.exception.RagException;
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
public class LlmClientImpl implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClientImpl.class);
    private static final String LLM_ERROR_CODE = "LLM_ERROR";

    private final WebClient webClient;
    private final String chatModel;
    private final int maxTokens;
    private final double temperature;
    private final RagConcurrencyLimiter concurrencyLimiter;
    private final ObjectMapper objectMapper;

    public LlmClientImpl(
            @Qualifier("lmStudioWebClient") WebClient webClient,
            LmStudioProperties properties,
            RagConcurrencyLimiter concurrencyLimiter,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.chatModel = properties.chatModel();
        this.maxTokens = properties.maxTokens();
        this.temperature = properties.chatTemperature();
        this.concurrencyLimiter = concurrencyLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String ask(String prompt) {
        return concurrencyLimiter.withChatPermit(() -> doAsk(prompt));
    }

    private String doAsk(String prompt) {
        Utf8EncodingDebugLogger.logPromptToLmStudio(prompt);

        log.info(
                "LM Studio chat request model={} promptLength={} maxTokens={} temperature={}",
                chatModel,
                prompt.length(),
                maxTokens,
                temperature
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
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
                                                "LM Studio error status={} body={}",
                                                clientResponse.statusCode(),
                                                errorBody
                                        );
                                        return Mono.error(new RagException(
                                                buildLlmErrorMessage(clientResponse.statusCode().value(), errorBody),
                                                LLM_ERROR_CODE
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
            log.error("LM Studio error status={} body={}", ex.getStatusCode(), errorBody);
            throw new RagException(
                    buildLlmErrorMessage(ex.getStatusCode().value(), errorBody),
                    LLM_ERROR_CODE,
                    ex
            );
        } catch (WebClientRequestException ex) {
            log.error("LM Studio connection failed: {}", ex.getMessage(), ex);
            throw new RagException("LM Studio connection failed: " + ex.getMessage(), LLM_ERROR_CODE, ex);
        } catch (Exception ex) {
            log.error("LM Studio request failed: {}", ex.getMessage(), ex);
            throw new RagException("LM Studio request failed: " + ex.getMessage(), LLM_ERROR_CODE, ex);
        }
    }

    private String parseSuccessResponse(byte[] responseBytes) {
        if (responseBytes == null || responseBytes.length == 0) {
            throw new RagException("LM Studio returned an empty response", LLM_ERROR_CODE);
        }

        String json = new String(responseBytes, StandardCharsets.UTF_8);
        Utf8EncodingDebugLogger.logLmStudioRawResponseBody(json);

        JsonNode response;
        try {
            response = objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new RagException("LM Studio response JSON is invalid", LLM_ERROR_CODE, ex);
        }

        if (response.has("error")) {
            String errorMessage = response.path("error").path("message").asText("Unknown LM Studio error");
            throw new RagException("LM Studio error: " + errorMessage, LLM_ERROR_CODE);
        }

        if (!response.has("choices")
                || !response.get("choices").isArray()
                || response.get("choices").isEmpty()) {
            throw new RagException("LM Studio response has no choices", LLM_ERROR_CODE);
        }

        JsonNode contentNode = response.get("choices").get(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new RagException("LM Studio response content is empty", LLM_ERROR_CODE);
        }

        return contentNode.asText().trim();
    }

    private String buildLlmErrorMessage(int statusCode, String errorBody) {
        if (errorBody == null || errorBody.isBlank()) {
            return "LM Studio returned HTTP " + statusCode;
        }
        String compactBody = errorBody.length() > 500 ? errorBody.substring(0, 497) + "..." : errorBody;
        return "LM Studio returned HTTP " + statusCode + ": " + compactBody;
    }
}
