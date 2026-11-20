package com.aislam.rag.service;

import com.aislam.rag.config.ChatProperties;
import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.dto.ChatQuotaResponse;
import com.aislam.rag.entity.AppUserEntity;
import com.aislam.rag.entity.ChatDailyUsageEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.AppUserRepository;
import com.aislam.rag.repository.ChatDailyUsageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ChatQuotaService {

    private final AppUserRepository appUserRepository;
    private final ChatDailyUsageRepository chatDailyUsageRepository;
    private final ChatProperties chatProperties;
    private final AdsConfigService adsConfigService;
    private final ZoneId zoneId;

    public ChatQuotaService(
            AppUserRepository appUserRepository,
            ChatDailyUsageRepository chatDailyUsageRepository,
            ChatProperties chatProperties,
            AdsConfigService adsConfigService,
            DailyProperties dailyProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.chatDailyUsageRepository = chatDailyUsageRepository;
        this.chatProperties = chatProperties;
        this.adsConfigService = adsConfigService;
        this.zoneId = dailyProperties.zoneId();
    }

    private int rewardedDailyMax() {
        return adsConfigService.effectiveRewardedDailyMax(chatProperties.rewardedDailyMax());
    }

    @Transactional(readOnly = true)
    public ChatQuotaResponse getQuota(UUID appUserId) {
        AppUserEntity appUser = appUserRepository.findById(appUserId).orElse(null);
        LocalDate today = LocalDate.now(zoneId);
        ChatDailyUsageEntity usage = chatDailyUsageRepository
                .findByAppUserIdAndUsageDate(appUserId, today)
                .orElse(null);
        int used = usage == null ? 0 : usage.getCount();
        int rewardedBonus = usage == null ? 0 : usage.getRewardedBonus();
        boolean premium = appUser != null && isPremiumActive(appUser);
        int limit = resolveDailyLimit(premium, rewardedBonus);
        int remaining = Math.max(limit - used, 0);
        int rewardedRemaining = premium
                ? 0
                : Math.max(rewardedDailyMax() - rewardedBonus, 0);

        return new ChatQuotaResponse(
                limit,
                used,
                remaining,
                premium,
                startOfNextDay(today),
                rewardedBonus,
                rewardedRemaining
        );
    }

    /**
     * İstemci claim (cihaz/JWT + günlük limit).
     * AdMob SSV gecikse veya gelmese de reklam sonrası hak düşmesin.
     * SSV callback ayrıca gelirse {@link #claimRewardedBonusFromSsv} receipt ile tekilleşir;
     * ikinci artış günlük max ile sınırlıdır.
     */
    @Transactional
    public ChatQuotaResponse claimRewardedBonus(UUID appUserId) {
        return incrementRewardedBonus(appUserId);
    }

    /**
     * AdMob SSV callback — imza doğrulandıktan sonra.
     * İstemci zaten claim ettiyse günlük max dolana kadar ek artar; aşım sessizce atlanır.
     */
    @Transactional
    public ChatQuotaResponse claimRewardedBonusFromSsv(UUID appUserId) {
        try {
            return incrementRewardedBonus(appUserId);
        } catch (RagException ex) {
            if ("CHAT_REWARD_LIMIT".equals(ex.getCode())) {
                return getQuota(appUserId);
            }
            throw ex;
        }
    }

    private ChatQuotaResponse incrementRewardedBonus(UUID appUserId) {
        AppUserEntity appUser = ensureAppUser(appUserId);
        if (isPremiumActive(appUser)) {
            return getQuota(appUserId);
        }

        LocalDate today = LocalDate.now(zoneId);
        ensureUsageRow(appUserId, today);

        int updated = chatDailyUsageRepository.tryIncrementRewardedBonus(
                appUserId,
                today,
                rewardedDailyMax(),
                Instant.now()
        );
        if (updated == 0) {
            throw new RagException(
                    "Bugün için ek soru hakkınız doldu. Yarın tekrar deneyebilirsiniz.",
                    "CHAT_REWARD_LIMIT"
            );
        }
        return getQuota(appUserId);
    }

    /**
     * Atomically reserves one daily question. Call {@link #releaseQuota(UUID)} if the ask fails.
     */
    @Transactional
    public void reserveQuota(UUID appUserId) {
        AppUserEntity appUser = ensureAppUser(appUserId);
        LocalDate today = LocalDate.now(zoneId);
        ensureUsageRow(appUserId, today);
        int rewardedBonus = chatDailyUsageRepository
                .findByAppUserIdAndUsageDate(appUserId, today)
                .map(ChatDailyUsageEntity::getRewardedBonus)
                .orElse(0);
        int limit = resolveDailyLimit(isPremiumActive(appUser), rewardedBonus);

        int updated = chatDailyUsageRepository.tryIncrement(appUserId, today, limit, Instant.now());
        if (updated == 0) {
            throw new RagException(
                    "Günlük soru hakkınız doldu. Premium ile günde "
                            + chatProperties.premiumDailyLimit()
                            + " soru sorabilirsiniz.",
                    "CHAT_QUOTA_EXCEEDED"
            );
        }
    }

    @Transactional
    public void releaseQuota(UUID appUserId) {
        LocalDate today = LocalDate.now(zoneId);
        chatDailyUsageRepository.tryDecrement(appUserId, today, Instant.now());
    }

    /** @deprecated Prefer {@link #reserveQuota(UUID)} + {@link #releaseQuota(UUID)}. */
    @Transactional
    public void consumeQuota(UUID appUserId) {
        reserveQuota(appUserId);
    }

    @Transactional
    public AppUserEntity ensureAppUser(UUID appUserId) {
        return appUserRepository.findById(appUserId)
                .orElseGet(() -> appUserRepository.save(new AppUserEntity(appUserId)));
    }

    @Transactional
    public void updatePremium(UUID appUserId, boolean premium, Instant premiumExpiresAt) {
        AppUserEntity appUser = ensureAppUser(appUserId);
        appUser.setPremium(premium);
        appUser.setPremiumExpiresAt(premiumExpiresAt);
        appUserRepository.save(appUser);
    }

    public boolean isPremiumActive(AppUserEntity appUser) {
        if (!appUser.isPremium()) {
            return false;
        }
        Instant expiresAt = appUser.getPremiumExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    private void ensureUsageRow(UUID appUserId, LocalDate today) {
        if (chatDailyUsageRepository.findByAppUserIdAndUsageDate(appUserId, today).isPresent()) {
            return;
        }
        try {
            chatDailyUsageRepository.saveAndFlush(new ChatDailyUsageEntity(appUserId, today));
        } catch (DataIntegrityViolationException ignored) {
            // concurrent insert of the same day row
        }
    }

    private int resolveDailyLimit(boolean premium, int rewardedBonus) {
        if (premium) {
            return chatProperties.premiumDailyLimit();
        }
        return chatProperties.freeDailyLimit() + Math.max(rewardedBonus, 0);
    }

    private Instant startOfNextDay(LocalDate today) {
        return today.plusDays(1).atStartOfDay(zoneId).toInstant();
    }
}
