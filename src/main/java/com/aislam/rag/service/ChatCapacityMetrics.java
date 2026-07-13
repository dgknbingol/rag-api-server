package com.aislam.rag.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Component
public class ChatCapacityMetrics {

    private final AtomicLong activeChatRequests = new AtomicLong();
    private final AtomicLong waitingChatRequests = new AtomicLong();
    private final LongAdder totalChatRequests = new LongAdder();
    private final LongAdder chatBusyRejections = new LongAdder();
    private final LongAdder chatCompleted = new LongAdder();
    private final LongAdder chatFailures = new LongAdder();
    private final LongAdder totalLatencyMs = new LongAdder();
    private final AtomicLong jobsPending = new AtomicLong();
    private final AtomicLong jobsRunning = new AtomicLong();
    private final LongAdder jobsCompleted = new LongAdder();
    private final LongAdder jobsFailed = new LongAdder();

    public void onChatWaitStart() {
        waitingChatRequests.incrementAndGet();
        totalChatRequests.increment();
    }

    public void onChatWaitEnd() {
        waitingChatRequests.decrementAndGet();
    }

    public void onChatActiveStart() {
        activeChatRequests.incrementAndGet();
    }

    public void onChatActiveEnd(long latencyMs, boolean success) {
        activeChatRequests.decrementAndGet();
        totalLatencyMs.add(Math.max(0, latencyMs));
        if (success) {
            chatCompleted.increment();
        } else {
            chatFailures.increment();
        }
    }

    public void onChatBusy() {
        chatBusyRejections.increment();
    }

    public void setJobsPending(long value) {
        jobsPending.set(value);
    }

    public void setJobsRunning(long value) {
        jobsRunning.set(value);
    }

    public void onJobCompleted() {
        jobsCompleted.increment();
    }

    public void onJobFailed() {
        jobsFailed.increment();
    }

    public ChatCapacitySnapshot snapshot(int maxConcurrent, int maxQueue, long requestTimeoutSeconds) {
        long completed = chatCompleted.sum();
        long avgLatency = completed == 0 ? 0 : totalLatencyMs.sum() / completed;
        return new ChatCapacitySnapshot(
                activeChatRequests.get(),
                waitingChatRequests.get(),
                maxConcurrent,
                maxQueue,
                requestTimeoutSeconds,
                totalChatRequests.sum(),
                chatBusyRejections.sum(),
                completed,
                chatFailures.sum(),
                avgLatency,
                jobsPending.get(),
                jobsRunning.get(),
                jobsCompleted.sum(),
                jobsFailed.sum()
        );
    }

    public record ChatCapacitySnapshot(
            long activeChatRequests,
            long waitingChatRequests,
            int maxConcurrentChatRequests,
            int maxChatQueueSize,
            long requestTimeoutSeconds,
            long totalChatRequests,
            long chatBusyRejections,
            long chatCompleted,
            long chatFailures,
            long avgLatencyMs,
            long jobsPending,
            long jobsRunning,
            long jobsCompleted,
            long jobsFailed
    ) {
    }
}
