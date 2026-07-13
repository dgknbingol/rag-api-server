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
    private final ZoneId zoneId;

    public ChatQuotaService(
            AppUserRepository appUserRepository,
            ChatDailyUsageRepository chatDailyUsageRepository,
            ChatProperties chatProperties,
            DailyProperties dailyProperties
    ) {
        this.appUserRepository = appUserRepository;
        this.chatDailyUsageRepository = chatDailyUsageRepository;
        this.chatProperties = chatProperties;
        this.zoneId = dailyProperties.zoneId();
    }

    @Transactional(readOnly = true)
    public ChatQuotaResponse getQuota(UUID appUserId) {
        AppUserEntity appUser = appUserRepository.findById(appUserId).orElse(null);
        LocalDate today = LocalDate.now(zoneId);
        int used = getUsedCount(appUserId, today);
        int limit = appUser == null
                ? chatProperties.freeDailyLimit()
                : resolveDailyLimit(appUser);
        int remaining = Math.max(limit - used, 0);

        return new ChatQuotaResponse(
                limit,
                used,
                remaining,
                appUser != null && isPremiumActive(appUser),
                startOfNextDay(today)
        );
    }

    /**
     * Atomically reserves one daily question. Call {@link #releaseQuota(UUID)} if the ask fails.
     */
    @Transactional
    public void reserveQuota(UUID appUserId) {
        AppUserEntity appUser = ensureAppUser(appUserId);
        LocalDate today = LocalDate.now(zoneId);
        int limit = resolveDailyLimit(appUser);
        ensureUsageRow(appUserId, today);

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

    private int resolveDailyLimit(AppUserEntity appUser) {
        return isPremiumActive(appUser)
                ? chatProperties.premiumDailyLimit()
                : chatProperties.freeDailyLimit();
    }

    private int getUsedCount(UUID appUserId, LocalDate today) {
        return chatDailyUsageRepository.findByAppUserIdAndUsageDate(appUserId, today)
                .map(ChatDailyUsageEntity::getCount)
                .orElse(0);
    }

    private Instant startOfNextDay(LocalDate today) {
        return today.plusDays(1).atStartOfDay(zoneId).toInstant();
    }
}
