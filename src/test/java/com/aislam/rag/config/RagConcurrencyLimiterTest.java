package com.aislam.rag.config;

import com.aislam.rag.exception.RagException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagConcurrencyLimiterTest {

    private static RagProperties chatLimiterProperties() {
        return new RagProperties("keyword", 30, 5, 200, 1, 8000, 80, 1800, 250, 1, 4, 1, 1000, 3000, true, 5, 0.12, 0.7, 0.55);
    }

    private static RagProperties embeddingLimiterProperties() {
        return new RagProperties("keyword", 30, 5, 200, 1, 8000, 80, 1800, 250, 1, 2, 1, 1000, 3000, true, 5, 0.12, 0.7, 0.55);
    }

    @Test
    void chatPermitTimesOutWhenAllSlotsTaken() throws InterruptedException {
        RagConcurrencyLimiter limiter = new RagConcurrencyLimiter(chatLimiterProperties());

        CountDownLatch chatStarted = new CountDownLatch(1);
        CountDownLatch releaseChat = new CountDownLatch(1);
        AtomicBoolean secondFailed = new AtomicBoolean(false);

        Thread holder = new Thread(() -> limiter.withChatPermit(() -> {
            chatStarted.countDown();
            try {
                releaseChat.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        }));
        holder.start();

        assertTrue(chatStarted.await(2, TimeUnit.SECONDS));

        Thread waiter = new Thread(() -> {
            try {
                limiter.withChatPermit(() -> "should-not-run");
            } catch (RagException ex) {
                secondFailed.set(true);
                assertEquals(RagConcurrencyLimiter.BUSY_MESSAGE, ex.getMessage());
            }
        });
        waiter.start();
        waiter.join(3000);

        releaseChat.countDown();
        holder.join(3000);

        assertTrue(secondFailed.get());
    }

    @Test
    void embeddingPermitAllowsParallelismUpToLimit() throws InterruptedException {
        RagConcurrencyLimiter limiter = new RagConcurrencyLimiter(embeddingLimiterProperties());

        CountDownLatch twoRunning = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean thirdFailed = new AtomicBoolean(false);

        Runnable hold = () -> limiter.withEmbeddingPermit(() -> {
            twoRunning.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        Thread first = new Thread(hold);
        Thread second = new Thread(hold);
        first.start();
        second.start();

        assertTrue(twoRunning.await(2, TimeUnit.SECONDS));

        Thread third = new Thread(() -> {
            try {
                limiter.withEmbeddingPermit(() -> "blocked");
            } catch (RagException ex) {
                thirdFailed.set(true);
            }
        });
        third.start();
        third.join(3000);

        release.countDown();
        first.join(3000);
        second.join(3000);

        assertTrue(thirdFailed.get());
    }
}
