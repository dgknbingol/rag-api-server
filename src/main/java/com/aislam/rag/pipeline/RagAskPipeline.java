package com.aislam.rag.pipeline;

import com.aislam.rag.debug.Utf8EncodingDebugLogger;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.provider.ProviderIds;
import com.aislam.rag.provider.ProviderRegistry;
import com.aislam.rag.service.ChunkContextExpander;
import com.aislam.rag.service.ConversationalQueryDetector;
import com.aislam.rag.service.PromptBuilder;
import com.aislam.rag.service.RagAskFlowLogger;
import com.aislam.rag.service.RagRetrievalService;
import com.aislam.rag.service.SourceRelevanceEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagAskPipeline implements AskPipeline {

    private static final Logger log = LoggerFactory.getLogger(RagAskPipeline.class);

    private final ProviderRegistry providerRegistry;
    private final RagRetrievalService ragRetrievalService;
    private final PromptBuilder promptBuilder;
    private final ChunkContextExpander chunkContextExpander;
    private final SourceRelevanceEvaluator sourceRelevanceEvaluator;
    private final ConversationalQueryDetector conversationalQueryDetector;
    private final RagAskFlowLogger ragAskFlowLogger;

    public RagAskPipeline(
            ProviderRegistry providerRegistry,
            RagRetrievalService ragRetrievalService,
            PromptBuilder promptBuilder,
            ChunkContextExpander chunkContextExpander,
            SourceRelevanceEvaluator sourceRelevanceEvaluator,
            ConversationalQueryDetector conversationalQueryDetector,
            RagAskFlowLogger ragAskFlowLogger
    ) {
        this.providerRegistry = providerRegistry;
        this.ragRetrievalService = ragRetrievalService;
        this.promptBuilder = promptBuilder;
        this.chunkContextExpander = chunkContextExpander;
        this.sourceRelevanceEvaluator = sourceRelevanceEvaluator;
        this.conversationalQueryDetector = conversationalQueryDetector;
        this.ragAskFlowLogger = ragAskFlowLogger;
    }

    @Override
    public String id() {
        return ProviderIds.PIPELINE_RAG;
    }

    @Override
    public AskResponse ask(String question) {
        if (conversationalQueryDetector.isConversational(question)) {
            String answer = conversationalQueryDetector.respond(question);
            Utf8EncodingDebugLogger.logFinalApiAnswer(answer);
            return new AskResponse(answer, List.of());
        }

        List<SourceChunk> rankedChunks = ragRetrievalService.retrieve(question);
        ragAskFlowLogger.logRetrievedSources(question, rankedChunks);

        if (!sourceRelevanceEvaluator.isRelevantEnough(question, rankedChunks)) {
            String answer = SourceRelevanceEvaluator.INSUFFICIENT_SOURCE_ANSWER;
            log.info("Ask flow stopped at relevance gate question={}", question);
            Utf8EncodingDebugLogger.logFinalApiAnswer(answer);
            return new AskResponse(answer, ragRetrievalService.toSearchResults(rankedChunks));
        }

        List<SourceChunk> promptChunks = chunkContextExpander.expand(rankedChunks);
        if (promptChunks.isEmpty()) {
            promptChunks = rankedChunks;
            log.info(
                    "Ask flow using rankedChunks directly because expand returned empty count={}",
                    rankedChunks.size()
            );
        }

        String prompt = promptBuilder.buildPrompt(question, promptChunks);
        ragAskFlowLogger.logPromptContext(promptChunks, prompt);
        String answer = providerRegistry.llm().complete(prompt);
        Utf8EncodingDebugLogger.logFinalApiAnswer(answer);
        return new AskResponse(answer, ragRetrievalService.toSearchResults(rankedChunks));
    }
}
