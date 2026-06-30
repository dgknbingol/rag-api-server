package com.aislam.rag.pipeline;

import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.provider.ProviderIds;
import com.aislam.rag.provider.ProviderRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DirectAskPipeline implements AskPipeline {

    private final ProviderRegistry providerRegistry;

    public DirectAskPipeline(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public String id() {
        return ProviderIds.PIPELINE_DIRECT;
    }

    @Override
    public AskResponse ask(String question) {
        String answer = providerRegistry.llm().chat(question);
        return new AskResponse(answer, List.of());
    }
}
