package com.aislam.rag.service;

import com.aislam.rag.client.RerankerClient;
import com.aislam.rag.config.RerankingProperties;
import com.aislam.rag.domain.SourceChunk;
import com.aislam.rag.util.TurkishTextNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class KeywordReranker implements RerankerClient {

    private static final Logger log = LoggerFactory.getLogger(KeywordReranker.class);

    private final RerankingProperties properties;
    private final Set<String> stopWords;

    public KeywordReranker(RerankingProperties properties) {
        this.properties = properties;
        this.stopWords = new HashSet<>(
                properties.stopWords() != null && !properties.stopWords().isEmpty()
                        ? properties.stopWords()
                        : RerankingProperties.defaultStopWords()
        );
    }

    @Override
    public List<SourceChunk> rerank(String question, List<SourceChunk> candidates, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<String> keywords = TurkishTextNormalizer.extractKeywords(
                question,
                properties.minKeywordLength(),
                stopWords
        );
        List<String> phrases = TurkishTextNormalizer.extractPhrases(
                question,
                properties.minPhraseWords(),
                properties.maxPhraseWords(),
                properties.minKeywordLength(),
                stopWords
        );

        log.debug("Rerank questionKeywords={} questionPhrases={}", keywords, phrases);

        List<SourceChunk> reranked = new ArrayList<>(candidates.size());
        for (SourceChunk candidate : candidates) {
            reranked.add(scoreCandidate(candidate, keywords, phrases));
        }

        reranked.sort(Comparator.comparingDouble(SourceChunk::score).reversed());
        if (reranked.size() <= limit) {
            return reranked;
        }
        return reranked.subList(0, limit);
    }

    private SourceChunk scoreCandidate(SourceChunk candidate, List<String> keywords, List<String> phrases) {
        String content = candidate.content() != null ? candidate.content() : "";
        String normalizedContent = TurkishTextNormalizer.normalize(content);
        String sectionTitle = candidate.sectionTitle() != null ? candidate.sectionTitle() : "";
        String normalizedSectionTitle = TurkishTextNormalizer.normalize(sectionTitle);
        String documentTitle = candidate.title() != null ? candidate.title() : "";
        String normalizedDocumentTitle = TurkishTextNormalizer.normalize(documentTitle);

        double keywordScore = calculateKeywordScore(keywords, normalizedContent, properties.keywordFrequencyWeight(), properties.keywordScoreMax());
        double phraseScore = calculatePhraseScore(phrases, normalizedContent, properties.phraseScorePerMatch(), properties.phraseScoreMax());
        double sectionTitleBoost = calculateSectionTitleBoost(keywords, phrases, normalizedSectionTitle)
                + calculateSectionTitleBoost(keywords, phrases, normalizedDocumentTitle);
        double qualityPenalty = calculateQualityPenalty(content, normalizedContent);
        double vectorScore = candidate.vectorScore() != null ? candidate.vectorScore() : 0.0;
        double finalScore = vectorScore + keywordScore + phraseScore + sectionTitleBoost - qualityPenalty;

        log.debug(
                "Rerank chunk documentId={} chunkIndex={} sectionTitle={} sectionTitleBoost={}",
                candidate.documentId(),
                candidate.chunkIndex(),
                candidate.sectionTitle(),
                sectionTitleBoost
        );

        return candidate.withRerankScores(keywordScore, phraseScore, sectionTitleBoost, qualityPenalty, finalScore);
    }

    private double calculateSectionTitleBoost(List<String> keywords, List<String> phrases, String normalizedSectionTitle) {
        if (normalizedSectionTitle.isBlank()) {
            return 0.0;
        }

        double keywordBoost = calculateKeywordScore(
                keywords,
                normalizedSectionTitle,
                properties.sectionTitleKeywordWeight(),
                properties.sectionTitleKeywordScoreMax()
        );
        double phraseBoost = calculatePhraseScore(
                phrases,
                normalizedSectionTitle,
                properties.sectionTitlePhraseScorePerMatch(),
                properties.sectionTitlePhraseScoreMax()
        );
        return keywordBoost + phraseBoost;
    }

    private double calculateKeywordScore(
            List<String> keywords,
            String normalizedText,
            double frequencyWeight,
            double scoreMax
    ) {
        if (keywords.isEmpty() || normalizedText.isBlank()) {
            return 0.0;
        }

        double score = 0.0;
        for (String keyword : keywords) {
            int occurrences = TurkishTextNormalizer.countTermOccurrences(normalizedText, keyword);
            if (occurrences > 0) {
                score += occurrences * frequencyWeight;
            }
        }
        return Math.min(score, scoreMax);
    }

    private double calculatePhraseScore(
            List<String> phrases,
            String normalizedText,
            double scorePerMatch,
            double scoreMax
    ) {
        if (phrases.isEmpty() || normalizedText.isBlank()) {
            return 0.0;
        }

        double score = 0.0;
        for (String phrase : phrases) {
            if (normalizedText.contains(phrase)) {
                score += scorePerMatch;
            }
        }
        return Math.min(score, scoreMax);
    }

    private double calculateQualityPenalty(String rawContent, String normalizedContent) {
        double penalty = 0.0;

        if (isTableOfContents(rawContent, normalizedContent)) {
            penalty += properties.tocPenalty();
        }

        double digitRatio = TurkishTextNormalizer.digitRatio(rawContent);
        if (digitRatio > properties.numberDensityThreshold()) {
            penalty += properties.numberDensityPenalty();
        }

        int pageReferences = TurkishTextNormalizer.countPageReferences(rawContent);
        if (pageReferences >= properties.pageReferenceThreshold()) {
            penalty += properties.pageReferencePenalty();
        }

        double letterRatio = TurkishTextNormalizer.letterRatio(rawContent);
        if (letterRatio < properties.minLetterRatio()) {
            penalty += properties.lowSemanticTextPenalty();
        }

        return penalty;
    }

    private boolean isTableOfContents(String rawContent, String normalizedContent) {
        if (normalizedContent.contains("içindekiler") || normalizedContent.contains("fihrist")) {
            return true;
        }

        return TurkishTextNormalizer.tableOfContentsLineRatio(rawContent) >= properties.tocDotLeaderLineRatio();
    }
}
