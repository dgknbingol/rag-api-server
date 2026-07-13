package com.aislam.rag.service;

import com.aislam.rag.config.AskProperties;
import com.aislam.rag.config.ChatProperties;
import com.aislam.rag.config.DeepSeekProperties;
import com.aislam.rag.dto.ChatConversationSummaryDto;
import com.aislam.rag.dto.ChatMessageDto;
import com.aislam.rag.dto.ChatMessageResponseDto;
import com.aislam.rag.entity.ChatConversationEntity;
import com.aislam.rag.entity.ChatMessageEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.provider.ProviderRegistry;
import com.aislam.rag.provider.llm.LlmProvider;
import com.aislam.rag.repository.ChatConversationRepository;
import com.aislam.rag.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ChatConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatQuotaService chatQuotaService;
    private final ChatProperties chatProperties;
    private final ProviderRegistry providerRegistry;
    private final DeepSeekProperties deepSeekProperties;
    private final AskProperties askProperties;
    private final TransactionTemplate transactionTemplate;

    public ChatConversationService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            ChatQuotaService chatQuotaService,
            ChatProperties chatProperties,
            ProviderRegistry providerRegistry,
            DeepSeekProperties deepSeekProperties,
            AskProperties askProperties,
            TransactionTemplate transactionTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatQuotaService = chatQuotaService;
        this.chatProperties = chatProperties;
        this.providerRegistry = providerRegistry;
        this.deepSeekProperties = deepSeekProperties;
        this.askProperties = askProperties;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public ChatConversationSummaryDto createConversation(UUID appUserId, String title) {
        chatQuotaService.ensureAppUser(appUserId);
        String resolvedTitle = (title == null || title.isBlank()) ? "Yeni sohbet" : title.trim();
        ChatConversationEntity entity = conversationRepository.save(
                new ChatConversationEntity(appUserId, resolvedTitle)
        );
        return toSummary(entity);
    }

    @Transactional(readOnly = true)
    public List<ChatConversationSummaryDto> listConversations(UUID appUserId) {
        return conversationRepository.findByAppUserIdOrderByUpdatedAtDesc(appUserId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> listMessages(UUID appUserId, UUID conversationId) {
        requireConversation(appUserId, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageDto)
                .toList();
    }

    @Transactional
    public void deleteConversation(UUID appUserId, UUID conversationId) {
        ChatConversationEntity conversation = requireConversation(appUserId, conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    /**
     * Sync ask that stores both user + assistant messages and returns the full pair.
     */
    public ChatMessageResponseDto postMessage(UUID appUserId, UUID conversationId, String content) {
        StringBuilder answerBuilder = new StringBuilder();
        ChatMessageEntity userMessage = prepareAndAsk(appUserId, conversationId, content, answerBuilder::append);
        ChatMessageEntity assistantMessage = persistAssistant(conversationId, answerBuilder.toString());
        return new ChatMessageResponseDto(
                toMessageDto(userMessage),
                toMessageDto(assistantMessage),
                conversationId
        );
    }

    /**
     * Streams deltas to the consumer, then persists the assistant reply.
     * Returns the saved assistant message.
     */
    public ChatMessageDto postMessageStreaming(
            UUID appUserId,
            UUID conversationId,
            String content,
            Consumer<String> onDelta
    ) {
        StringBuilder answerBuilder = new StringBuilder();
        prepareAndAsk(appUserId, conversationId, content, delta -> {
            answerBuilder.append(delta);
            onDelta.accept(delta);
        });
        ChatMessageEntity assistantMessage = persistAssistant(conversationId, answerBuilder.toString());
        return toMessageDto(assistantMessage);
    }

    public ChatMessageEntity prepareAndAsk(
            UUID appUserId,
            UUID conversationId,
            String content,
            Consumer<String> onDelta
    ) {
        String trimmed = content.trim();
        ChatMessageEntity userMessage = transactionTemplate.execute(status -> {
            ChatConversationEntity conversation = requireConversation(appUserId, conversationId);
            if (messageRepository.countByConversationId(conversationId) == 0) {
                conversation.setTitle(trimmed.length() > 40 ? trimmed.substring(0, 40) + "…" : trimmed);
            }
            conversation.touch();
            conversationRepository.save(conversation);
            return messageRepository.save(
                    new ChatMessageEntity(conversationId, ChatMessageEntity.Role.user, trimmed)
            );
        });

        chatQuotaService.reserveQuota(appUserId);
        try {
            List<Map<String, String>> llmMessages = buildLlmMessages(conversationId);
            LlmProvider llm = providerRegistry.llm();
            llm.streamChat(llmMessages, onDelta);
            return userMessage;
        } catch (RuntimeException ex) {
            chatQuotaService.releaseQuota(appUserId);
            throw ex;
        }
    }

    private ChatMessageEntity persistAssistant(UUID conversationId, String answer) {
        return transactionTemplate.execute(status -> {
            ChatConversationEntity conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RagException("Sohbet bulunamadı.", "CHAT_CONVERSATION_NOT_FOUND"));
            conversation.touch();
            conversationRepository.save(conversation);
            return messageRepository.save(
                    new ChatMessageEntity(conversationId, ChatMessageEntity.Role.assistant, answer.trim())
            );
        });
    }

    public List<Map<String, String>> buildLlmMessages(UUID conversationId) {
        List<ChatMessageEntity> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int max = chatProperties.maxContextMessages();
        if (history.size() > max) {
            history = history.subList(history.size() - max, history.size());
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", resolveSystemPrompt()));

        for (ChatMessageEntity item : history) {
            if (item.getRole() == ChatMessageEntity.Role.system) {
                continue;
            }
            Map<String, String> message = new HashMap<>();
            message.put("role", item.getRole().name());
            message.put("content", item.getContent());
            messages.add(message);
        }
        return messages;
    }

    private String resolveSystemPrompt() {
        if (deepSeekProperties.systemPrompt() != null && !deepSeekProperties.systemPrompt().isBlank()) {
            return deepSeekProperties.systemPrompt().trim();
        }
        if (askProperties.systemPrompt() != null && !askProperties.systemPrompt().isBlank()) {
            return askProperties.systemPrompt().trim();
        }
        return "Sen AiSLAM adlı bir İslami soru-cevap asistanısın.";
    }

    public ChatConversationEntity requireConversation(UUID appUserId, UUID conversationId) {
        return conversationRepository.findByIdAndAppUserId(conversationId, appUserId)
                .orElseThrow(() -> new RagException("Sohbet bulunamadı.", "CHAT_CONVERSATION_NOT_FOUND"));
    }

    private ChatConversationSummaryDto toSummary(ChatConversationEntity entity) {
        return new ChatConversationSummaryDto(
                entity.getId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChatMessageDto toMessageDto(ChatMessageEntity entity) {
        return new ChatMessageDto(
                entity.getId(),
                entity.getRole().name(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
