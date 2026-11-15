package com.aislam.rag.service;

import com.google.crypto.tink.apps.rewardedads.RewardedAdsVerifier;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

/** AdMob rewarded SSV imza doğrulama (Tink). */
@Service
public class AdMobSsvVerifier {

    private static final Logger log = LoggerFactory.getLogger(AdMobSsvVerifier.class);

    private RewardedAdsVerifier verifier;

    @PostConstruct
    void init() {
        try {
            verifier = new RewardedAdsVerifier.Builder()
                    .fetchVerifyingPublicKeysWith(RewardedAdsVerifier.KEYS_DOWNLOADER_INSTANCE_PROD)
                    .build();
            log.info("AdMob SSV verifier ready");
        } catch (GeneralSecurityException e) {
            log.error("AdMob SSV verifier init failed", e);
            verifier = null;
        }
    }

    public void verifyCallbackUrl(String fullUrl) {
        if (verifier == null) {
            throw new IllegalStateException("AdMob SSV verifier not initialized");
        }
        try {
            verifier.verify(fullUrl);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid SSV signature", e);
        }
    }
}
