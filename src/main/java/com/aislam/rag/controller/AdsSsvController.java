package com.aislam.rag.controller;

import com.aislam.rag.config.AdsSsvProperties;
import com.aislam.rag.entity.RewardedSsvReceiptEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.RewardedSsvReceiptRepository;
import com.aislam.rag.service.AdMobSsvVerifier;
import com.aislam.rag.service.ChatQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * AdMob rewarded Server-Side Verification callback.
 * AdMob Console → Rewarded ad unit → SSV callback URL:
 * {@code https://api.e-islam.net/api/ads/ssv/rewarded}
 */
@RestController
@RequestMapping("/api/ads/ssv")
public class AdsSsvController {

    private static final Logger log = LoggerFactory.getLogger(AdsSsvController.class);

    private final AdMobSsvVerifier ssvVerifier;
    private final AdsSsvProperties adsSsvProperties;
    private final RewardedSsvReceiptRepository receiptRepository;
    private final ChatQuotaService chatQuotaService;

    public AdsSsvController(
            AdMobSsvVerifier ssvVerifier,
            AdsSsvProperties adsSsvProperties,
            RewardedSsvReceiptRepository receiptRepository,
            ChatQuotaService chatQuotaService
    ) {
        this.ssvVerifier = ssvVerifier;
        this.adsSsvProperties = adsSsvProperties;
        this.receiptRepository = receiptRepository;
        this.chatQuotaService = chatQuotaService;
    }

    @GetMapping("/rewarded")
    @Transactional
    public ResponseEntity<String> rewarded(HttpServletRequest request) {
        if (!adsSsvProperties.enabled()) {
            return ResponseEntity.status(503).body("ssv_disabled");
        }

        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("missing_query");
        }

        // Ingress arkasında imza https public URL ile atılır; requestURL http olabilir.
        String fullUrl = publicRequestUrl(request) + "?" + query;
        try {
            ssvVerifier.verifyCallbackUrl(fullUrl);
        } catch (Exception ex) {
            log.warn("SSV signature invalid url={} err={}", fullUrl, ex.getMessage());
            return ResponseEntity.badRequest().body("invalid_signature");
        }

        String transactionId = request.getParameter("transaction_id");
        String userId = request.getParameter("user_id");
        String adUnit = request.getParameter("ad_unit");

        // AdMob "URL doğrula" tool user_id göndermeyebilir — imza OK ise 200 dön.
        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.ok("ok_verified_no_transaction");
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.ok("ok_verified_no_user");
        }

        String expectedUnit = adsSsvProperties.rewardedAdUnitId();
        if (expectedUnit != null && !expectedUnit.isBlank()
                && adUnit != null && !adUnit.isBlank()
                && !expectedUnit.equals(adUnit.trim())) {
            log.warn("SSV ad_unit mismatch got={} expected={}", adUnit, expectedUnit);
            // İmza geçerli; ödül verme ama AdMob verify için 200
            return ResponseEntity.ok("ok_verified_ad_unit_mismatch");
        }

        UUID appUserId;
        try {
            appUserId = UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            // Verify tool örnek userid_1234567 gönderebilir
            return ResponseEntity.ok("ok_verified_invalid_user");
        }

        if (receiptRepository.existsByTransactionId(transactionId.trim())) {
            return ResponseEntity.ok("duplicate");
        }

        receiptRepository.save(new RewardedSsvReceiptEntity(transactionId.trim(), appUserId, adUnit));

        try {
            chatQuotaService.claimRewardedBonusFromSsv(appUserId);
        } catch (RagException ex) {
            log.info("SSV grant skipped code={} user={}", ex.getCode(), appUserId);
        }

        return ResponseEntity.ok("ok");
    }

    /** X-Forwarded-* ile public https URL (Tink imza doğrulaması için). */
    static String publicRequestUrl(HttpServletRequest request) {
        String proto = firstHeader(request, "X-Forwarded-Proto");
        String host = firstHeader(request, "X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getHeader("Host");
        }
        if (proto != null && !proto.isBlank() && host != null && !host.isBlank()) {
            return proto.trim() + "://" + host.trim() + request.getRequestURI();
        }
        return request.getRequestURL().toString();
    }

    private static String firstHeader(HttpServletRequest request, String name) {
        String raw = request.getHeader(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int comma = raw.indexOf(',');
        return (comma >= 0 ? raw.substring(0, comma) : raw).trim();
    }
}
