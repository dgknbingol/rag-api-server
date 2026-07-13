package com.aislam.rag.service;

import com.aislam.rag.config.ChatProperties;
import com.aislam.rag.dto.ChatJobDto;
import com.aislam.rag.dto.CreateChatJobRequest;
import com.aislam.rag.entity.ChatConversationEntity;
import com.aislam.rag.entity.ChatJobEntity;
import com.aislam.rag.entity.ChatMessageEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.provider.ProviderRegistry;
import com.aislam.rag.repository.ChatConversationRepository;
import com.aislam.rag.repository.ChatJobRepository;
import com.aislam.rag.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatJobService {

    private static final Logger log = LoggerFactory.getLogger(ChatJobService.class);

    private final ChatJobRepository chatJobRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatConversationService conversationService;
    private final ChatQuotaService chatQuotaService;
    private final ChatProperties chatProperties;
    private final ChatCapacityMetrics metrics;
    private final ProviderRegistry providerRegistry;
    private final TransactionTemplate transactionTemplate;

    public ChatJobService(
            ChatJobRepository chatJobRepository,
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            ChatConversationService conversationService,
            ChatQuotaService chatQuotaService,
            ChatProperties chatProperties,
            ChatCapacityMetrics metrics,
            ProviderRegistry providerRegistry,
            TransactionTemplate transactionTemplate
    ) {
        this.chatJobRepository = chatJobRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.chatQuotaService = chatQuotaService;
        this.chatProperties = chatProperties;
        this.metrics = metrics;
        this.providerRegistry = providerRegistry;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ChatJobDto enqueue(UUID appUserId, CreateChatJobRequest request) {
        if (!chatProperties.jobsEnabled()) {
            throw new RagException("Chat job kuyruğu kapalı.", "CONFIG_ERROR");
        }

        chatQuotaService.ensureAppUser(appUserId);
        UUID conversationId = request.conversationId();
        if (conversationId != null) {
            conversationService.requireConversation(appUserId, conversationId);
        }

        chatQuotaService.reserveQuota(appUserId);
        try {
            ChatJobEntity job = chatJobRepository.save(
                    new ChatJobEntity(appUserId, conversationId, request.question().trim())
            );
            refreshJobMetrics();
            return toDto(job);
        } catch (RuntimeException ex) {
            chatQuotaService.releaseQuota(appUserId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public ChatJobDto getJob(UUID appUserId, UUID jobId) {
        ChatJobEntity job = chatJobRepository.findByIdAndAppUserId(jobId, appUserId)
                .orElseThrow(() -> new RagException("İş bulunamadı.", "CHAT_JOB_NOT_FOUND"));
        return toDto(job);
    }

    @Scheduled(fixedDelayString = "${app.chat.job-poll-interval-ms:1500}")
    public void pollAndProcess() {
        if (!chatProperties.jobsEnabled()) {
            return;
        }

        List<UUID> ids;
        try {
            ids = transactionTemplate.execute(status ->
                    chatJobRepository.lockPendingIds(chatProperties.jobBatchSize())
            );
        } catch (RuntimeException ex) {
            log.debug("Chat job claim skipped: {}", ex.getMessage());
            return;
        }

        if (ids == null || ids.isEmpty()) {
            refreshJobMetrics();
            return;
        }

        for (UUID id : ids) {
            processJob(id);
        }
        refreshJobMetrics();
    }

    private void processJob(UUID jobId) {
        ChatJobEntity claimed = transactionTemplate.execute(status -> {
            ChatJobEntity job = chatJobRepository.findById(jobId).orElse(null);
            if (job == null || job.getStatus() != ChatJobEntity.Status.PENDING) {
                return null;
            }
            job.markRunning();
            return chatJobRepository.save(job);
        });

        if (claimed == null) {
            return;
        }

        try {
            String answer = runLlm(claimed);
            transactionTemplate.executeWithoutResult(status -> {
                ChatJobEntity job = chatJobRepository.findById(jobId)
                        .orElseThrow(() -> new RagException("İş bulunamadı.", "CHAT_JOB_NOT_FOUND"));
                persistConversationSideEffects(job, answer);
                job.markDone(answer);
                chatJobRepository.save(job);
            });
            metrics.onJobCompleted();
        } catch (RuntimeException ex) {
            String code = ex instanceof RagException rag ? rag.getCode() : "CHAT_JOB_FAILED";
            String message = ex.getMessage() == null ? "Chat job failed" : ex.getMessage();
            transactionTemplate.executeWithoutResult(status -> {
                ChatJobEntity job = chatJobRepository.findById(jobId).orElse(null);
                if (job != null) {
                    job.markFailed(message, code);
                    chatJobRepository.save(job);
                }
            });
            chatQuotaService.releaseQuota(claimed.getAppUserId());
            metrics.onJobFailed();
            log.warn("Chat job {} failed: {}", jobId, message);
        }
    }

    private String runLlm(ChatJobEntity job) {
        if (job.getConversationId() != null) {
            transactionTemplate.executeWithoutResult(status -> {
                ChatConversationEntity conversation = conversationRepository
                        .findByIdAndAppUserId(job.getConversationId(), job.getAppUserId())
                        .orElseThrow(() -> new RagException("Sohbet bulunamadı.", "CHAT_CONVERSATION_NOT_FOUND"));
                if (messageRepository.countByConversationId(conversation.getId()) == 0) {
                    String title = job.getQuestion();
                    conversation.setTitle(title.length() > 40 ? title.substring(0, 40) + "…" : title);
                }
                conversation.touch();
                conversationRepository.save(conversation);
                messageRepository.save(new ChatMessageEntity(
                        conversation.getId(),
                        ChatMessageEntity.Role.user,
                        job.getQuestion()
                ));
            });

            List<Map<String, String>> messages = conversationService.buildLlmMessages(job.getConversationId());
            return providerRegistry.llm().chatMessages(messages);
        }

        return providerRegistry.llm().chat(job.getQuestion());
    }

    private void persistConversationSideEffects(ChatJobEntity job, String answer) {
        if (job.getConversationId() == null) {
            return;
        }
        ChatConversationEntity conversation = conversationRepository
                .findByIdAndAppUserId(job.getConversationId(), job.getAppUserId())
                .orElse(null);
        if (conversation == null) {
            return;
        }
        conversation.touch();
        conversationRepository.save(conversation);
        messageRepository.save(new ChatMessageEntity(
                conversation.getId(),
                ChatMessageEntity.Role.assistant,
                answer
        ));
    }

    private void refreshJobMetrics() {
        metrics.setJobsPending(chatJobRepository.countByStatus(ChatJobEntity.Status.PENDING));
        metrics.setJobsRunning(chatJobRepository.countByStatus(ChatJobEntity.Status.RUNNING));
    }

    private ChatJobDto toDto(ChatJobEntity job) {
        return new ChatJobDto(
                job.getId(),
                job.getConversationId(),
                job.getStatus().name(),
                job.getQuestion(),
                job.getAnswer(),
                job.getErrorMessage(),
                job.getErrorCode(),
                job.getCreatedAt(),
                job.getFinishedAt()
        );
    }
}
