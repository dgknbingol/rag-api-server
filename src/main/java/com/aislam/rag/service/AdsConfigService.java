package com.aislam.rag.service;

import com.aislam.rag.dto.AdsConfigDto;
import com.aislam.rag.entity.RemoteConfigEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.RemoteConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdsConfigService {

    public static final String ADS_CONFIG_KEY = "ads";

    private static final long MIN_COOLDOWN_MS = 10_000L;
    private static final long MAX_COOLDOWN_MS = 24 * 60 * 60 * 1000L;
    private static final int MIN_REWARDED_DAILY = 0;
    private static final int MAX_REWARDED_DAILY = 50;

    private static final Logger log = LoggerFactory.getLogger(AdsConfigService.class);

    private final RemoteConfigRepository remoteConfigRepository;
    private final ObjectMapper objectMapper;

    public AdsConfigService(RemoteConfigRepository remoteConfigRepository, ObjectMapper objectMapper) {
        this.remoteConfigRepository = remoteConfigRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AdsConfigDto getAdsConfig() {
        return remoteConfigRepository.findById(ADS_CONFIG_KEY)
                .map(this::parseOrDefault)
                .orElseGet(this::seedDefaults);
    }

    @Transactional
    public AdsConfigDto updateAdsConfig(AdsConfigDto incoming) {
        AdsConfigDto normalized = normalize(incoming);
        String json;
        try {
            json = objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new RagException("Ads config yazılamadı.", "ADS_CONFIG_WRITE");
        }

        RemoteConfigEntity entity = remoteConfigRepository.findById(ADS_CONFIG_KEY)
                .orElseGet(() -> new RemoteConfigEntity(ADS_CONFIG_KEY, json));
        entity.setConfigValue(json);
        remoteConfigRepository.save(entity);
        log.info("Ads config updated: {}", json);
        return normalized;
    }

    /** Quota / claim için efektif günlük rewarded tavanı (read-only; seed etmez). */
    @Transactional(readOnly = true)
    public int effectiveRewardedDailyMax(int fallback) {
        try {
            AdsConfigDto config = remoteConfigRepository.findById(ADS_CONFIG_KEY)
                    .map(this::parseOrDefault)
                    .orElseGet(AdsConfigDto::defaults);
            if (!config.rewardedEnabled()) {
                return 0;
            }
            return config.rewardedDailyMax();
        } catch (Exception e) {
            log.warn("Ads config read failed, using fallback rewardedDailyMax={}", fallback, e);
            return Math.max(fallback, 0);
        }
    }

    private AdsConfigDto seedDefaults() {
        AdsConfigDto defaults = AdsConfigDto.defaults();
        try {
            String json = objectMapper.writeValueAsString(defaults);
            remoteConfigRepository.saveAndFlush(new RemoteConfigEntity(ADS_CONFIG_KEY, json));
        } catch (DataIntegrityViolationException ignored) {
            return remoteConfigRepository.findById(ADS_CONFIG_KEY)
                    .map(this::parseOrDefault)
                    .orElse(defaults);
        } catch (Exception e) {
            log.warn("Failed to seed ads config", e);
        }
        return defaults;
    }

    private AdsConfigDto parseOrDefault(RemoteConfigEntity entity) {
        try {
            AdsConfigDto parsed = objectMapper.readValue(entity.getConfigValue(), AdsConfigDto.class);
            return normalize(parsed);
        } catch (Exception e) {
            log.warn("Invalid ads config JSON, using defaults", e);
            return AdsConfigDto.defaults();
        }
    }

    private AdsConfigDto normalize(AdsConfigDto raw) {
        AdsConfigDto base = raw == null ? AdsConfigDto.defaults() : raw;
        return new AdsConfigDto(
                base.bannerEnabled(),
                base.interstitialEnabled(),
                base.appOpenEnabled(),
                base.rewardedEnabled(),
                clamp(base.interstitialCooldownMs(), MIN_COOLDOWN_MS, MAX_COOLDOWN_MS),
                clamp(base.appOpenCooldownMs(), MIN_COOLDOWN_MS, MAX_COOLDOWN_MS),
                clampInt(base.rewardedDailyMax(), MIN_REWARDED_DAILY, MAX_REWARDED_DAILY)
        );
    }

    private static long clamp(long value, long min, long max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
