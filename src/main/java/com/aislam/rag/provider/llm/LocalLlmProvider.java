package com.aislam.rag.provider.llm;

import com.aislam.rag.client.LlmClient;
import com.aislam.rag.config.AskProperties;
import com.aislam.rag.provider.ProviderIds;
import org.springframework.stereotype.Component;

@Component
public class LocalLlmProvider implements LlmProvider {

    private final LlmClient llmClient;
    private final AskProperties askProperties;

    public LocalLlmProvider(LlmClient llmClient, AskProperties askProperties) {
        this.llmClient = llmClient;
        this.askProperties = askProperties;
    }

    @Override
    public String id() {
        return ProviderIds.LLM_LOCAL;
    }

    @Override
    public String chat(String question) {
        String systemPrompt = askProperties.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            return llmClient.ask(systemPrompt + "\n\nSoru: " + question);
        }
        return llmClient.ask(question);
    }

    @Override
    public String complete(String prompt) {
        return llmClient.ask(prompt);
    }
}
