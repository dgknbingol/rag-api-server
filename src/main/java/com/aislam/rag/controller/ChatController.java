package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.AskRequest;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.ChatQuotaResponse;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.AskOrchestratorService;
import com.aislam.rag.service.ChatQuotaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        value = "/api/chat",
        produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
        consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
)
public class ChatController {

    private final AskOrchestratorService askOrchestratorService;
    private final ChatQuotaService chatQuotaService;

    public ChatController(
            AskOrchestratorService askOrchestratorService,
            ChatQuotaService chatQuotaService
    ) {
        this.askOrchestratorService = askOrchestratorService;
        this.chatQuotaService = chatQuotaService;
    }

    @GetMapping("/quota")
    public ResponseEntity<ChatQuotaResponse> quota(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        return ResponseEntity.ok(chatQuotaService.getQuota(appUserId));
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody AskRequest request
    ) {
        UUID appUserId = parseAppUserId(appUserIdHeader);
        chatQuotaService.consumeQuota(appUserId);
        return ResponseEntity.ok(askOrchestratorService.ask(request.question()));
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
