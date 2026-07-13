package com.aislam.rag.config;

import com.aislam.rag.exception.RagException;
import com.aislam.rag.service.ChatCapacityMetrics;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RagConcurrencyLimiter {

    public static final String BUSY_MESSAGE =
            "Şu anda yoğunluk var. Lütfen birkaç saniye sonra tekrar deneyin.";
    public static final String QUEUE_FULL_MESSAGE =
            "Sohbet sırası dolu. Lütfen birazdan tekrar deneyin.";
    public static final String BUSY_CODE = "CHAT_BUSY";
    public static final String QUEUE_FULL_CODE = "CHAT_QUEUE_FULL";

    private final Semaphore chatSemaphore;
    private final Semaphore chatAdmissionSemaphore;
    private final Semaphore embeddingSemaphore;
    private final long requestTimeoutSeconds;
    private final int maxConcurrentChatRequests;
    private final int maxChatQueueSize;
    private final ChatCapacityMetrics metrics;

    public RagConcurrencyLimiter(RagProperties properties, ChatCapacityMetrics metrics) {
        this.maxConcurrentChatRequests = properties.maxConcurrentChatRequests();
        this.maxChatQueueSize = properties.maxChatQueueSize();
        this.chatSemaphore = new Semaphore(maxConcurrentChatRequests, true);
        // Admission = in-flight LLM + waiters. Fail-fast when queue is full.
        this.chatAdmissionSemaphore = new Semaphore(maxConcurrentChatRequests + maxChatQueueSize, true);
        this.embeddingSemaphore = new Semaphore(properties.maxConcurrentEmbeddingRequests(), true);
        this.requestTimeoutSeconds = properties.requestTimeoutSeconds();
        this.metrics = metrics;
    }

    public <T> T withChatPermit(Supplier<T> action) {
        if (!chatAdmissionSemaphore.tryAcquire()) {
            metrics.onChatBusy();
            throw new RagException(QUEUE_FULL_MESSAGE, QUEUE_FULL_CODE);
        }

        metrics.onChatWaitStart();
        boolean llmAcquired = false;
        long startedAt = System.currentTimeMillis();
        boolean success = false;
        try {
            llmAcquired = chatSemaphore.tryAcquire(requestTimeoutSeconds, TimeUnit.SECONDS);
            if (!llmAcquired) {
                metrics.onChatBusy();
                throw new RagException(BUSY_MESSAGE, BUSY_CODE);
            }

            metrics.onChatWaitEnd();
            metrics.onChatActiveStart();
            try {
                T result = action.get();
                success = true;
                return result;
            } finally {
                metrics.onChatActiveEnd(System.currentTimeMillis() - startedAt, success);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.onChatBusy();
            throw new RagException(BUSY_MESSAGE, BUSY_CODE, ex);
        } finally {
            if (!llmAcquired) {
                metrics.onChatWaitEnd();
            }
            if (llmAcquired) {
                chatSemaphore.release();
            }
            chatAdmissionSemaphore.release();
        }
    }

    public void withChatPermit(Runnable action) {
        withChatPermit(() -> {
            action.run();
            return null;
        });
    }

    public <T> T withEmbeddingPermit(Supplier<T> action) {
        boolean acquired = false;
        try {
            acquired = embeddingSemaphore.tryAcquire(requestTimeoutSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RagException(BUSY_MESSAGE, BUSY_CODE);
            }
            return action.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RagException(BUSY_MESSAGE, BUSY_CODE, ex);
        } finally {
            if (acquired) {
                embeddingSemaphore.release();
            }
        }
    }

    public int maxConcurrentChatRequests() {
        return maxConcurrentChatRequests;
    }

    public int maxChatQueueSize() {
        return maxChatQueueSize;
    }

    public long requestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
}
