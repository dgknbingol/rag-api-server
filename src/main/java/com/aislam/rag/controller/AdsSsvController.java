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

        String fullUrl = request.getRequestURL().append('?').append(query).toString();
        try {
            ssvVerifier.verifyCallbackUrl(fullUrl);
        } catch (Exception ex) {
            log.warn("SSV signature invalid: {}", ex.getMessage());
            return ResponseEntity.badRequest().body("invalid_signature");
        }

        String transactionId = request.getParameter("transaction_id");
        String userId = request.getParameter("user_id");
        String adUnit = request.getParameter("ad_unit");

        if (transactionId == null || transactionId.isBlank()) {
            return ResponseEntity.badRequest().body("missing_transaction");
        }
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body("missing_user");
        }

        String expectedUnit = adsSsvProperties.rewardedAdUnitId();
        if (expectedUnit != null && !expectedUnit.isBlank()
                && adUnit != null && !adUnit.isBlank()
                && !expectedUnit.equals(adUnit.trim())) {
            log.warn("SSV ad_unit mismatch got={} expected={}", adUnit, expectedUnit);
            return ResponseEntity.badRequest().body("ad_unit_mismatch");
        }

        if (receiptRepository.existsByTransactionId(transactionId.trim())) {
            return ResponseEntity.ok("duplicate");
        }

        UUID appUserId;
        try {
            appUserId = UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("invalid_user");
        }

        receiptRepository.save(new RewardedSsvReceiptEntity(transactionId.trim(), appUserId, adUnit));

        try {
            chatQuotaService.claimRewardedBonusFromSsv(appUserId);
        } catch (RagException ex) {
            // Limit dolu — receipt yine kaydedildi (replay yok); AdMob’a 200 dön.
            log.info("SSV grant skipped code={} user={}", ex.getCode(), appUserId);
        }

        return ResponseEntity.ok("ok");
    }
}
