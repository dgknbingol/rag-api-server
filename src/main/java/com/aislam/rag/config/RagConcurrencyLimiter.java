package com.aislam.rag.config;

import com.aislam.rag.exception.RagException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RagConcurrencyLimiter {

    public static final String BUSY_MESSAGE = "Şu anda yoğunluk var, lütfen birazdan tekrar deneyin.";

    private final Semaphore chatSemaphore;
    private final Semaphore embeddingSemaphore;
    private final long requestTimeoutSeconds;

    public RagConcurrencyLimiter(RagProperties properties) {
        this.chatSemaphore = new Semaphore(properties.maxConcurrentChatRequests(), true);
        this.embeddingSemaphore = new Semaphore(properties.maxConcurrentEmbeddingRequests(), true);
        this.requestTimeoutSeconds = properties.requestTimeoutSeconds();
    }

    public <T> T withChatPermit(Supplier<T> action) {
        return withPermit(chatSemaphore, action);
    }

    public void withChatPermit(Runnable action) {
        withChatPermit(() -> {
            action.run();
            return null;
        });
    }

    public <T> T withEmbeddingPermit(Supplier<T> action) {
        return withPermit(embeddingSemaphore, action);
    }

    private <T> T withPermit(Semaphore semaphore, Supplier<T> action) {
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(requestTimeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RagException(BUSY_MESSAGE);
            }
            return action.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RagException(BUSY_MESSAGE, ex);
        } finally {
            if (acquired) {
                semaphore.release();
            }
        }
    }
}
