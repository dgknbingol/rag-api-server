package com.aislam.rag.service;

import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.pipeline.AskPipeline;
import com.aislam.rag.provider.ProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AskOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AskOrchestratorService.class);

    private final ProviderRegistry providerRegistry;
    private final Map<String, AskPipeline> pipelines;

    public AskOrchestratorService(ProviderRegistry providerRegistry, List<AskPipeline> askPipelines) {
        this.providerRegistry = providerRegistry;
        this.pipelines = askPipelines.stream()
                .collect(Collectors.toUnmodifiableMap(AskPipeline::id, Function.identity()));
    }

    public AskResponse ask(String question) {
        String pipelineId = providerRegistry.pipelineId();
        AskPipeline pipeline = pipelines.get(pipelineId);
        if (pipeline == null) {
            throw new RagException(
                    "Unknown ask pipeline '" + pipelineId + "'. Available: " + pipelines.keySet(),
                    "CONFIG_ERROR"
            );
        }

        log.info(
                "Ask orchestrator pipeline={} llm={} embedding={}",
                pipelineId,
                providerRegistry.llmId(),
                providerRegistry.embeddingId()
        );

        return pipeline.ask(question);
    }
}
