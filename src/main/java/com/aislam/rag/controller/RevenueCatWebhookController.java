package com.aislam.rag.controller;

import com.aislam.rag.config.RevenueCatProperties;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.RevenueCatWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/revenuecat")
public class RevenueCatWebhookController {

    private final RevenueCatProperties properties;
    private final RevenueCatWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public RevenueCatWebhookController(
            RevenueCatProperties properties,
            RevenueCatWebhookService webhookService,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String body
    ) {
        if (properties.isWebhookConfigured()) {
            String expected = properties.webhookAuthorization();
            if (authorization == null || !authorization.equals(expected)) {
                throw new RagException("RevenueCat webhook yetkisiz.", "WEBHOOK_UNAUTHORIZED");
            }
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            webhookService.handleEvent(root);
        } catch (RagException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RagException("RevenueCat webhook işlenemedi.", "WEBHOOK_INVALID");
        }

        return ResponseEntity.ok().build();
    }
}
