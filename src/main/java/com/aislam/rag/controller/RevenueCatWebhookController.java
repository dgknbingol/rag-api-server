package com.aislam.rag.controller;

import com.aislam.rag.config.RevenueCatProperties;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.RevenueCatWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/revenuecat")
public class RevenueCatWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookController.class);

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
            if (!authorizationMatches(authorization, expected)) {
                // Secret değerini loglama — sadece var mı / uzunluk.
                log.warn(
                        "RevenueCat webhook auth mismatch receivedPresent={} receivedLen={} expectedLen={}",
                        authorization != null && !authorization.isBlank(),
                        authorization == null ? 0 : authorization.trim().length(),
                        expected == null ? 0 : expected.trim().length()
                );
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

    /**
     * RevenueCat Authorization alanına "Bearer …" veya sadece secret yazılabilir;
     * trim + Bearer öneki farklarını tolere et.
     */
    private static boolean authorizationMatches(String received, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (received == null) {
            return false;
        }
        String r = received.trim();
        String e = expected.trim();
        if (r.equals(e)) {
            return true;
        }
        return stripBearerPrefix(r).equals(stripBearerPrefix(e));
    }

    private static String stripBearerPrefix(String value) {
        String current = value.trim();
        while (true) {
            if (current.length() >= 7 && current.regionMatches(true, 0, "Bearer ", 0, 7)) {
                current = current.substring(7).trim();
                continue;
            }
            // "Bearer" + token boşluksuz (RC/UI bazen böyle gönderir) → 32 vs 33
            if (current.length() > 6
                    && current.regionMatches(true, 0, "Bearer", 0, 6)
                    && !Character.isWhitespace(current.charAt(6))) {
                current = current.substring(6).trim();
                continue;
            }
            break;
        }
        return current;
    }
}
