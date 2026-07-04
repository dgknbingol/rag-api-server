package com.aislam.rag.service;

import com.aislam.rag.config.RevenueCatProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RevenueCatWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookService.class);

    private final RevenueCatProperties properties;
    private final ChatQuotaService chatQuotaService;

    public RevenueCatWebhookService(
            RevenueCatProperties properties,
            ChatQuotaService chatQuotaService
    ) {
        this.properties = properties;
        this.chatQuotaService = chatQuotaService;
    }

    public void handleEvent(JsonNode root) {
        JsonNode event = root.path("event");
        if (event.isMissingNode()) {
            log.warn("RevenueCat webhook missing event payload");
            return;
        }

        String appUserIdRaw = textValue(event, "app_user_id");
        if (appUserIdRaw == null || appUserIdRaw.isBlank()) {
            log.warn("RevenueCat webhook missing app_user_id");
            return;
        }

        UUID appUserId;
        try {
            appUserId = UUID.fromString(appUserIdRaw);
        } catch (IllegalArgumentException ex) {
            log.warn("RevenueCat webhook app_user_id is not UUID: {}", appUserIdRaw);
            return;
        }

        String eventType = textValue(event, "type");
        Instant expiration = parseExpiration(event);

        boolean premium = shouldGrantPremium(eventType, event, expiration);
        chatQuotaService.updatePremium(appUserId, premium, premium ? expiration : null);

        log.info(
                "RevenueCat webhook processed type={} appUserId={} premium={} expiresAt={}",
                eventType,
                appUserId,
                premium,
                expiration
        );
    }

    private boolean shouldGrantPremium(String eventType, JsonNode event, Instant expiration) {
        if ("EXPIRATION".equals(eventType)) {
            return false;
        }

        if (!hasPremiumEntitlement(event)) {
            return false;
        }

        if (expiration != null && expiration.isBefore(Instant.now())) {
            return false;
        }

        return switch (eventType) {
            case "INITIAL_PURCHASE",
                 "RENEWAL",
                 "UNCANCELLATION",
                 "NON_RENEWING_PURCHASE",
                 "PRODUCT_CHANGE",
                 "SUBSCRIPTION_EXTENDED",
                 "TEMPORARY_ENTITLEMENT_GRANT" -> true;
            case "CANCELLATION" -> expiration == null || expiration.isAfter(Instant.now());
            default -> expiration != null && expiration.isAfter(Instant.now());
        };
    }

    private boolean hasPremiumEntitlement(JsonNode event) {
        JsonNode entitlementIds = event.path("entitlement_ids");
        if (entitlementIds.isArray()) {
            for (JsonNode entitlementId : entitlementIds) {
                if (properties.premiumEntitlementId().equals(entitlementId.asText())) {
                    return true;
                }
            }
        }

        JsonNode entitlementId = event.path("entitlement_id");
        return properties.premiumEntitlementId().equals(entitlementId.asText(null));
    }

    private Instant parseExpiration(JsonNode event) {
        JsonNode expirationMs = event.path("expiration_at_ms");
        if (expirationMs.isNumber()) {
            return Instant.ofEpochMilli(expirationMs.asLong());
        }
        return null;
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
