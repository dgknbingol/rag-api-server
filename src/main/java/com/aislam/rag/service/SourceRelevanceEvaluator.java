package com.aislam.rag.service;

import com.aislam.rag.config.RagProperties;
import com.aislam.rag.config.RerankingProperties;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.util.TurkishTextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Blocks the LLM only when no retrieved chunk is semantically or lexically usable.
 * Vector similarity is trusted at {@code relevanceGateMinVectorScore} even when keywordScore is 0.
 */
@Component
public class SourceRelevanceEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SourceRelevanceEvaluator.class);

    public static final String INSUFFICIENT_SOURCE_ANSWER = "Bu konuda yeterli kaynak bulunamadı.";

    private final boolean enabled;
    private final int topChunksToCheck;
    private final double minLexicalScore;
    private final double minUsableVectorScore;
    private final int minKeywordLength;
    private final int minPhraseWords;
    private final int maxPhraseWords;
    private final Set<String> stopWords;

    public SourceRelevanceEvaluator(RagProperties ragProperties, RerankingProperties rerankingProperties) {
        this.enabled = ragProperties.relevanceGateEnabled();
        this.topChunksToCheck = Math.max(1, ragProperties.relevanceGateTopChunks());
        this.minLexicalScore = ragProperties.relevanceGateMinLexicalScore();
        this.minUsableVectorScore = ragProperties.relevanceGateMinVectorScore();
        this.minKeywordLength = rerankingProperties.minKeywordLength();
        this.minPhraseWords = rerankingProperties.minPhraseWords();
        this.maxPhraseWords = rerankingProperties.maxPhraseWords();
        this.stopWords = new HashSet<>(
                rerankingProperties.stopWords() != null && !rerankingProperties.stopWords().isEmpty()
                        ? rerankingProperties.stopWords()
                        : RerankingProperties.defaultStopWords()
        );
    }

    public boolean isRelevantEnough(String question, List<SourceChunk> rankedChunks) {
        if (!enabled) {
            return true;
        }
        if (rankedChunks == null || rankedChunks.isEmpty()) {
            log.info("Relevance gate rejected: no ranked chunks for question={}", question);
            return false;
        }

        List<String> keywords = TurkishTextNormalizer.extractKeywords(
                question,
                minKeywordLength,
                stopWords
        );
        List<String> phrases = TurkishTextNormalizer.extractPhrases(
                question,
                minPhraseWords,
                maxPhraseWords,
                minKeywordLength,
                stopWords
        );

        int limit = Math.min(topChunksToCheck, rankedChunks.size());
        for (int i = 0; i < limit; i++) {
            SourceChunk chunk = rankedChunks.get(i);
            if (isUsableChunk(chunk, keywords, phrases)) {
                log.debug(
                        "Relevance gate passed index={} vectorScore={} keywordScore={} sectionTitle={}",
                        i,
                        chunk.vectorScore(),
                        chunk.keywordScore(),
                        chunk.sectionTitle()
                );
                return true;
            }
        }

        log.info(
                "Relevance gate rejected question={} keywords={} phrases={} topVectorScore={} topKeywordScore={}",
                question,
                keywords,
                phrases,
                rankedChunks.getFirst().vectorScore(),
                rankedChunks.getFirst().keywordScore()
        );
        return false;
    }

    private boolean isUsableChunk(SourceChunk chunk, List<String> keywords, List<String> phrases) {
        if (hasUsableVectorScore(chunk)) {
            return true;
        }

        String normalizedCombined = TurkishTextNormalizer.normalize(combinedSearchText(chunk));
        if (normalizedCombined.isBlank()) {
            return false;
        }

        if (!phrases.isEmpty() && containsAnyPhrase(phrases, normalizedCombined)) {
            return true;
        }

        if (lexicalScore(chunk) >= minLexicalScore) {
            return true;
        }

        if (keywords.isEmpty()) {
            return false;
        }

        int matchedKeywords = countMatchedKeywords(keywords, normalizedCombined);
        if (keywords.size() == 1) {
            return matchedKeywords == 1;
        }
        return matchedKeywords >= keywords.size();
    }

    private boolean hasUsableVectorScore(SourceChunk chunk) {
        Double vectorScore = chunk.vectorScore();
        return vectorScore != null && vectorScore >= minUsableVectorScore;
    }

    private static double lexicalScore(SourceChunk chunk) {
        double keywordScore = chunk.keywordScore() != null ? chunk.keywordScore() : 0.0;
        double phraseScore = chunk.phraseScore() != null ? chunk.phraseScore() : 0.0;
        double sectionBoost = chunk.sectionTitleBoost() != null ? chunk.sectionTitleBoost() : 0.0;
        return keywordScore + phraseScore + sectionBoost;
    }

    private static boolean containsAnyPhrase(List<String> phrases, String normalizedCombined) {
        for (String phrase : phrases) {
            if (normalizedCombined.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static int countMatchedKeywords(List<String> keywords, String normalizedCombined) {
        int matched = 0;
        for (String keyword : keywords) {
            if (TurkishTextNormalizer.termMatches(normalizedCombined, keyword)) {
                matched++;
            }
        }
        return matched;
    }

    private static String combinedSearchText(SourceChunk chunk) {
        StringBuilder combined = new StringBuilder();
        appendIfPresent(combined, chunk.title());
        appendIfPresent(combined, chunk.sectionTitle());
        appendIfPresent(combined, chunk.content());
        return combined.toString();
    }

    private static void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value);
        }
    }
}
