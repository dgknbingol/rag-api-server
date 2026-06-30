package com.aislam.rag.provider.llm;

public interface LlmProvider {

    String id();

    /** Doğrudan sohbet: kullanıcı sorusu → cevap (RAG yok). */
    String chat(String question);

    /** RAG modu: hazır prompt → cevap. */
    String complete(String prompt);
}
