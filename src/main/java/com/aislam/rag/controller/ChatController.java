package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.config.RagConcurrencyLimiter;
import com.aislam.rag.dto.AskRequest;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.ChatConversationSummaryDto;
import com.aislam.rag.dto.ChatJobDto;
import com.aislam.rag.dto.ChatMessageDto;
import com.aislam.rag.dto.ChatMessageResponseDto;
import com.aislam.rag.dto.ChatQuotaResponse;
import com.aislam.rag.dto.CreateChatJobRequest;
import com.aislam.rag.dto.CreateConversationRequest;
import com.aislam.rag.dto.PostChatMessageRequest;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.AskOrchestratorService;
import com.aislam.rag.service.ChatCapacityMetrics;
import com.aislam.rag.service.ChatConversationService;
import com.aislam.rag.service.ChatJobService;
import com.aislam.rag.service.ChatQuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping(value = "/api/chat")
public class ChatController {

    private final AskOrchestratorService askOrchestratorService;
    private final ChatQuotaService chatQuotaService;
    private final ChatConversationService chatConversationService;
    private final ChatJobService chatJobService;
    private final ChatCapacityMetrics chatCapacityMetrics;
    private final RagConcurrencyLimiter concurrencyLimiter;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public ChatController(
            AskOrchestratorService askOrchestratorService,
            ChatQuotaService chatQuotaService,
            ChatConversationService chatConversationService,
            ChatJobService chatJobService,
            ChatCapacityMetrics chatCapacityMetrics,
            RagConcurrencyLimiter concurrencyLimiter,
            ObjectMapper objectMapper
    ) {
        this.askOrchestratorService = askOrchestratorService;
        this.chatQuotaService = chatQuotaService;
        this.chatConversationService = chatConversationService;
        this.chatJobService = chatJobService;
        this.chatCapacityMetrics = chatCapacityMetrics;
        this.concurrencyLimiter = concurrencyLimiter;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/quota", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ChatQuotaResponse> quota(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(chatQuotaService.getQuota(appUserId));
    }

    @GetMapping(value = "/capacity", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ChatCapacityMetrics.ChatCapacitySnapshot> capacity() {
        return ResponseEntity.ok(chatCapacityMetrics.snapshot(
                concurrencyLimiter.maxConcurrentChatRequests(),
                concurrencyLimiter.maxChatQueueSize(),
                concurrencyLimiter.requestTimeoutSeconds()
        ));
    }

    @PostMapping(
            value = "/ask",
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<AskResponse> ask(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody AskRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        chatQuotaService.reserveQuota(appUserId);
        try {
            return ResponseEntity.ok(askOrchestratorService.ask(request.question()));
        } catch (RuntimeException ex) {
            chatQuotaService.releaseQuota(appUserId);
            throw ex;
        }
    }

    @PostMapping(
            value = "/conversations",
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<ChatConversationSummaryDto> createConversation(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody(required = false) CreateConversationRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        String title = request == null ? null : request.title();
        return ResponseEntity.ok(chatConversationService.createConversation(appUserId, title));
    }

    @GetMapping(value = "/conversations", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<ChatConversationSummaryDto>> listConversations(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(chatConversationService.listConversations(appUserId));
    }

    @GetMapping(
            value = "/conversations/{conversationId}/messages",
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<List<ChatMessageDto>> listMessages(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @PathVariable UUID conversationId
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(chatConversationService.listMessages(appUserId, conversationId));
    }

    @DeleteMapping(value = "/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @PathVariable UUID conversationId
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        chatConversationService.deleteConversation(appUserId, conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/conversations/{conversationId}/messages",
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<ChatMessageResponseDto> postMessage(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @PathVariable UUID conversationId,
            @Valid @RequestBody PostChatMessageRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(
                chatConversationService.postMessage(appUserId, conversationId, request.content())
        );
    }

    @PostMapping(
            value = "/conversations/{conversationId}/messages/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public SseEmitter postMessageStream(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @PathVariable UUID conversationId,
            @Valid @RequestBody PostChatMessageRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        SseEmitter emitter = new SseEmitter(180_000L);

        sseExecutor.execute(() -> {
            try {
                ChatMessageDto assistant = chatConversationService.postMessageStreaming(
                        appUserId,
                        conversationId,
                        request.content(),
                        delta -> sendEvent(emitter, "delta", Map.of("text", delta))
                );
                sendEvent(emitter, "done", Map.of(
                        "messageId", assistant.id().toString(),
                        "content", assistant.content(),
                        "createdAt", assistant.createdAt().toString()
                ));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    String code = ex instanceof RagException rag ? rag.getCode() : "CHAT_STREAM_ERROR";
                    String message = ex.getMessage() == null ? "Stream failed" : ex.getMessage();
                    sendEvent(emitter, "error", Map.of("message", message, "code", code));
                } catch (Exception ignored) {
                    // emitter may already be closed
                }
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    @PostMapping(
            value = "/jobs",
            produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<ChatJobDto> enqueueJob(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody CreateChatJobRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.accepted().body(chatJobService.enqueue(appUserId, request));
    }

    @GetMapping(value = "/jobs/{jobId}", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ChatJobDto> getJob(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @PathVariable UUID jobId
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(chatJobService.getJob(appUserId, jobId));
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(name)
                    .data(objectMapper.writeValueAsString(data), MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            throw new RagException("SSE gönderimi başarısız.", "CHAT_STREAM_ERROR", ex);
        }
    }

    private UUID parseAppUserId(String appUserIdHeader) {
        if (appUserIdHeader == null || appUserIdHeader.isBlank()) {
            throw new RagException("X-App-User-Id başlığı gerekli.", "APP_USER_ID_REQUIRED");
        }
        try {
            return UUID.fromString(appUserIdHeader.trim());
        } catch (IllegalArgumentException ex) {
            throw new RagException("Geçersiz X-App-User-Id.", "APP_USER_ID_INVALID");
        }
    }
}
