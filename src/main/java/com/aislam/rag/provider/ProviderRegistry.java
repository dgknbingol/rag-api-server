package com.aislam.rag.provider;

import com.aislam.rag.config.AskProperties;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.provider.embedding.EmbeddingProvider;
import com.aislam.rag.provider.llm.LlmProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final AskProperties askProperties;
    private final Map<String, LlmProvider> llmProviders;
    private final Map<String, EmbeddingProvider> embeddingProviders;

    public ProviderRegistry(
            AskProperties askProperties,
            List<LlmProvider> llmProviders,
            List<EmbeddingProvider> embeddingProviders
    ) {
        this.askProperties = askProperties;
        this.llmProviders = llmProviders.stream()
                .collect(Collectors.toUnmodifiableMap(LlmProvider::id, Function.identity()));
        this.embeddingProviders = embeddingProviders.stream()
                .collect(Collectors.toUnmodifiableMap(EmbeddingProvider::id, Function.identity()));
    }

    public LlmProvider llm() {
        return resolve(llmProviders, askProperties.llm(), "LLM");
    }

    public EmbeddingProvider embedding() {
        return resolve(embeddingProviders, askProperties.embedding(), "embedding");
    }

    public String pipelineId() {
        return askProperties.pipeline();
    }

    public String llmId() {
        return askProperties.llm();
    }

    public String embeddingId() {
        return askProperties.embedding();
    }

    private static <T> T resolve(Map<String, T> providers, String id, String label) {
        T provider = providers.get(id);
        if (provider == null) {
            throw new RagException(
                    "Unknown " + label + " provider '" + id + "'. Available: " + providers.keySet(),
                    "CONFIG_ERROR"
            );
        }
        return provider;
    }
}
