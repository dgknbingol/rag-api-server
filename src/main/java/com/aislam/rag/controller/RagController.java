package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.debug.Utf8EncodingDebugLogger;
import com.aislam.rag.dto.AskRequest;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.SearchResultDto;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        value = "/api/rag",
        produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
        consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
)
public class RagController {

    private final RagService ragService;
    private final ChatQuotaService chatQuotaService;

    public RagController(RagService ragService, ChatQuotaService chatQuotaService) {
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

    @PostMapping("/retrieve")
    public ResponseEntity<List<SearchResultDto>> retrieve(
            @RequestHeader(value = "X-App-User-Id", required = false) String appUserIdHeader,
            @Valid @RequestBody AskRequest request
    ) {
        RequestAppUserId.require(appUserIdHeader);
        return ResponseEntity.ok(ragService.retrieveSources(request.question()));
    }
}
