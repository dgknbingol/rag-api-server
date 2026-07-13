package com.aislam.rag.provider.llm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface LlmProvider {

    String id();

    /** Doğrudan sohbet: kullanıcı sorusu → cevap (RAG yok). */
    String chat(String question);

    /** RAG modu: hazır prompt → cevap. */
    String complete(String prompt);

    /** Çok turlu sohbet (role/content mesaj listesi). */
    default String chatMessages(List<Map<String, String>> messages) {
        String lastUser = "";
        for (Map<String, String> message : messages) {
            if ("user".equals(message.get("role"))) {
                lastUser = message.getOrDefault("content", "");
            }
        }
        return chat(lastUser);
    }

    /**
     * Akan cevap. Varsayılan implementasyon tek seferde üretir ve tek parça olarak iletir.
     */
    default void streamChat(List<Map<String, String>> messages, Consumer<String> onDelta) {
        String answer = chatMessages(messages);
        if (answer != null && !answer.isBlank()) {
            onDelta.accept(answer);
        }
    }
}
