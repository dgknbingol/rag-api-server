package com.aislam.rag.service;

import com.aislam.rag.exception.RagException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tek kullanımlık device-session challenge (replay engeli). */
@Service
public class DeviceChallengeService {

    private static final long TTL_SECONDS = 300;

    private record Entry(UUID deviceId, Instant expiresAt) {
    }

    private final Map<String, Entry> challenges = new ConcurrentHashMap<>();

    public String issue(UUID deviceId) {
        purgeExpired();
        String challenge = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        challenges.put(challenge, new Entry(deviceId, Instant.now().plusSeconds(TTL_SECONDS)));
        return challenge;
    }

    public long ttlSeconds() {
        return TTL_SECONDS;
    }

    /** Challenge’ı tüketir; geçersizse exception. */
    public void consume(String challenge, UUID deviceId) {
        if (challenge == null || challenge.isBlank()) {
            throw new RagException("Challenge gerekli.", "DEVICE_CHALLENGE_REQUIRED");
        }
        Entry entry = challenges.remove(challenge.trim());
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new RagException("Challenge geçersiz veya süresi dolmuş.", "DEVICE_CHALLENGE_INVALID");
        }
        if (!entry.deviceId().equals(deviceId)) {
            throw new RagException("Challenge cihazı eşleşmiyor.", "DEVICE_CHALLENGE_MISMATCH");
        }
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        challenges.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }
}
