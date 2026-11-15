package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.debug.Utf8EncodingDebugLogger;
import com.aislam.rag.dto.AskRequest;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.security.RequestAppUserId;
import com.aislam.rag.service.ChatQuotaService;
import com.aislam.rag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Backward-compatible endpoint. Prefer {@link RagController} at /api/rag/ask.
 */
@RestController
@RequestMapping(
        value = "/api/questions",
        produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
        consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
)
public class QuestionController {

    private final RagService ragService;
    private final ChatQuotaService chatQuotaService;

    public QuestionController(RagService ragService, ChatQuotaService chatQuotaService) {
        this.ragService = ragService;
        this.chatQuotaService = chatQuotaService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody AskRequest request
    ) {
        UUID appUserId = RequestAppUserId.require(appUserIdHeader);
        Utf8EncodingDebugLogger.logControllerQuestion(request.question());
        chatQuotaService.reserveQuota(appUserId);
        try {
            return ResponseEntity.ok(ragService.ask(request.question()));
        } catch (RuntimeException ex) {
            chatQuotaService.releaseQuota(appUserId);
            throw ex;
        }
    }
}
