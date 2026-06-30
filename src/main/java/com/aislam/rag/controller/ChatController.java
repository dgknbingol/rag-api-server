package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.AskRequest;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.service.AskOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/api/chat",
        produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE,
        consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
)
public class ChatController {

    private final AskOrchestratorService askOrchestratorService;

    public ChatController(AskOrchestratorService askOrchestratorService) {
        this.askOrchestratorService = askOrchestratorService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(askOrchestratorService.ask(request.question()));
    }
}
