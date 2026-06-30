package com.aislam.rag.service;

import com.aislam.rag.client.QdrantClient;
import com.aislam.rag.client.RerankerClient;
import com.aislam.rag.config.RagProperties;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.dto.AskResponse;
import com.aislam.rag.dto.SearchResultDto;
import com.aislam.rag.pipeline.RagAskPipeline;
import com.aislam.rag.provider.ProviderRegistry;
import com.aislam.rag.provider.embedding.EmbeddingProvider;
import com.aislam.rag.provider.llm.LlmProvider;
import com.aislam.rag.service.ChunkContextExpander;
import com.aislam.rag.service.ConversationalQueryDetector;
import com.aislam.rag.service.PromptBuilder;
import com.aislam.rag.service.RagAskFlowLogger;
import com.aislam.rag.service.SectionTitleResolver;
import com.aislam.rag.service.SourceRelevanceEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceImplTest {

    private EmbeddingProvider embeddingProvider;
    private QdrantClient qdrantClient;
    private PromptBuilder promptBuilder;
    private LlmProvider llmProvider;
    private RerankerClient rerankerClient;
    private ChunkContextExpander chunkContextExpander;
    private SectionTitleResolver sectionTitleResolver;
    private SourceRelevanceEvaluator sourceRelevanceEvaluator;
    private ConversationalQueryDetector conversationalQueryDetector;
    private RagAskFlowLogger ragAskFlowLogger;
    private ProviderRegistry providerRegistry;
    private RagAskPipeline ragAskPipeline;
    private RagRetrievalService ragRetrievalService;

    @BeforeEach
    void setUp() {
        embeddingProvider = mock(EmbeddingProvider.class);
        qdrantClient = mock(QdrantClient.class);
        promptBuilder = mock(PromptBuilder.class);
        llmProvider = mock(LlmProvider.class);
        rerankerClient = mock(RerankerClient.class);
        chunkContextExpander = mock(ChunkContextExpander.class);
        sectionTitleResolver = mock(SectionTitleResolver.class);
        sourceRelevanceEvaluator = mock(SourceRelevanceEvaluator.class);
        conversationalQueryDetector = new ConversationalQueryDetector();
        ragAskFlowLogger = mock(RagAskFlowLogger.class);
        providerRegistry = mock(ProviderRegistry.class);

        when(providerRegistry.llm()).thenReturn(llmProvider);
        when(providerRegistry.embedding()).thenReturn(embeddingProvider);

        ragRetrievalService = new RagRetrievalService(
                providerRegistry,
                qdrantClient,
                rerankerClient,
                sectionTitleResolver,
                RagProperties.forTests()
        );

        ragAskPipeline = new RagAskPipeline(
                providerRegistry,
                ragRetrievalService,
                promptBuilder,
                chunkContextExpander,
                sourceRelevanceEvaluator,
                conversationalQueryDetector,
                ragAskFlowLogger
        );

        when(sectionTitleResolver.resolve(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sourceRelevanceEvaluator.isRelevantEnough(any(), any())).thenReturn(true);
    }

    @Test
    void ask_returnsGreetingWithoutRetrievalForSelam() {
        AskResponse response = ragAskPipeline.ask("selam");

        assertTrue(response.answer().contains("Aleykümselam") || response.answer().contains("aleykümselam"));
        assertEquals(0, response.sources().size());
        verify(embeddingProvider, never()).embed(any());
    }

    @Test
    void ask_usesExpandedContextForPromptButReturnsRankedSources() {
        String question = "Abdesti bozan şeyler nelerdir?";
        float[] vector = new float[]{0.1f, 0.2f};
        List<SourceChunk> candidates = List.of(
                SourceChunk.fromVectorSearch("3", "doc-1", "Abdest", "İlmihal", "3. Kanal suyu.", 12, 2, 0.91)
        );
        List<SourceChunk> rankedChunks = List.of(candidates.getFirst());
        List<SourceChunk> expandedChunks = List.of(
                SourceChunk.fromVectorSearch("2", "doc-1", "Abdest", "İlmihal", "2. Yüz yıkanır.", 12, 1, 0.0),
                SourceChunk.fromVectorSearch("3", "doc-1", "Abdest", "İlmihal", "3. Kanal suyu.", 12, 2, 0.91),
                SourceChunk.fromVectorSearch("4", "doc-1", "Abdest", "İlmihal", "4. Baş mesh edilir.", 12, 3, 0.0)
        );

        when(embeddingProvider.embed(question)).thenReturn(vector);
        when(qdrantClient.search(vector, 30)).thenReturn(candidates);
        when(rerankerClient.rerank(question, candidates, 5)).thenReturn(rankedChunks);
        when(chunkContextExpander.expand(rankedChunks)).thenReturn(expandedChunks);
        when(promptBuilder.buildPrompt(eq(question), eq(expandedChunks))).thenReturn("prompt");
        when(llmProvider.complete("prompt")).thenReturn("Abdesti bozan haller şunlardır.");

        AskResponse response = ragAskPipeline.ask(question);

        assertEquals("Abdesti bozan haller şunlardır.", response.answer());
        assertEquals(1, response.sources().size());
        verify(sectionTitleResolver).resolve(candidates);
        verify(promptBuilder).buildPrompt(question, expandedChunks);
    }

    @Test
    void retrieveSources_returnsSectionTitleAndDebugScores() {
        String question = "Namaz nedir?";
        float[] vector = new float[]{0.3f};
        List<SourceChunk> candidates = List.of(
                SourceChunk.fromVectorSearch(
                        "2", "doc-2", "Namaz", "İlmihal", "NAMAZIN FARZLARI",
                        "Namaz İslam'ın şartlarındandır.", 15, 3, 0.88
                )
        );
        List<SourceChunk> ranked = List.of(
                candidates.getFirst().withRerankScores(0.05, 0.10, 0.20, 0.0, 1.23)
        );

        when(embeddingProvider.embed(question)).thenReturn(vector);
        when(qdrantClient.search(vector, 30)).thenReturn(candidates);
        when(rerankerClient.rerank(question, candidates, 5)).thenReturn(ranked);

        List<SearchResultDto> results = ragRetrievalService.retrieveSources(question);

        assertEquals(1, results.size());
        SearchResultDto result = results.getFirst();
        assertEquals("NAMAZIN FARZLARI", result.sectionTitle());
        assertNotNull(result.contentPreview());
        assertEquals(0.20, result.sectionTitleBoost());
        assertEquals(
                result.vectorScore() + result.keywordScore() + result.phraseScore()
                        + result.sectionTitleBoost() - result.qualityPenalty(),
                result.finalScore()
        );
    }

    @Test
    void ask_skipsLlmWhenRelevanceGateFails() {
        String question = "İslam'ın şartları nelerdir?";
        float[] vector = new float[]{0.1f};
        List<SourceChunk> rankedChunks = List.of(
                SourceChunk.fromVectorSearch(
                        "1", "doc", "Namaz İlmihali", "Diyanet",
                        "Cuma namazı farz-ı kifayedir.",
                        "Cuma Namazı", 40, 12, 0.91
                )
        );

        when(embeddingProvider.embed(question)).thenReturn(vector);
        when(qdrantClient.search(vector, 30)).thenReturn(rankedChunks);
        when(rerankerClient.rerank(question, rankedChunks, 5)).thenReturn(rankedChunks);
        when(sourceRelevanceEvaluator.isRelevantEnough(question, rankedChunks)).thenReturn(false);

        AskResponse response = ragAskPipeline.ask(question);

        assertEquals(SourceRelevanceEvaluator.INSUFFICIENT_SOURCE_ANSWER, response.answer());
        assertEquals(1, response.sources().size());
        verify(llmProvider, never()).complete(any());
        verify(chunkContextExpander, never()).expand(any());
    }

    @Test
    void retrieve_searchesInitialCandidatesThenReranksToFinalCount() {
        String question = "Namaz nedir?";
        float[] vector = new float[]{0.3f};
        List<SourceChunk> candidates = List.of(
                SourceChunk.fromVectorSearch(
                        "2", "doc-2", "Namaz", "İlmihal",
                        "Namaz İslam'ın şartlarındandır.", 15, 3, 0.88
                )
        );

        when(embeddingProvider.embed(question)).thenReturn(vector);
        when(qdrantClient.search(vector, 30)).thenReturn(candidates);
        when(rerankerClient.rerank(question, candidates, 5)).thenReturn(candidates);

        List<SourceChunk> result = ragRetrievalService.retrieve(question);

        assertEquals(1, result.size());
        verify(qdrantClient).search(vector, 30);
        verify(sectionTitleResolver).resolve(candidates);
        verify(rerankerClient).rerank(question, candidates, 5);
    }
}
