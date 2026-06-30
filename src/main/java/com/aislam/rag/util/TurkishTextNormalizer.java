package com.aislam.rag.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TurkishTextNormalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{P}\\p{S}]+");
    private static final Pattern TERM = Pattern.compile("[\\p{L}\\p{M}]+");
    private static final Pattern PAGE_REFERENCE = Pattern.compile(
            "(?i)(?:\\bs\\.\\s*\\d+|\\bsayfa\\s+\\d+|\\bsf\\.\\s*\\d+|\\bs\\.\\s*\\d+)"
    );

    private TurkishTextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String nfc = Normalizer.normalize(text, Normalizer.Form.NFC);
        String lowered = nfc.toLowerCase(TURKISH);
        String withoutPunctuation = PUNCTUATION.matcher(lowered).replaceAll(" ");
        return withoutPunctuation.replaceAll("\\s+", " ").strip();
    }

    public static List<String> extractKeywords(String text, int minLength, Set<String> stopWords) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = TERM.matcher(normalize(text));
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() >= minLength && !stopWords.contains(term)) {
                keywords.add(term);
            }
        }
        return List.copyOf(keywords);
    }

    public static List<String> extractPhrases(
            String text,
            int minPhraseWords,
            int maxPhraseWords,
            int minKeywordLength,
            Set<String> stopWords
    ) {
        List<String> terms = extractKeywordSequence(text, minKeywordLength, stopWords);
        if (terms.size() < minPhraseWords) {
            return List.of();
        }

        Set<String> phrases = new LinkedHashSet<>();
        for (int size = minPhraseWords; size <= Math.min(maxPhraseWords, terms.size()); size++) {
            for (int i = 0; i <= terms.size() - size; i++) {
                phrases.add(String.join(" ", terms.subList(i, i + size)));
            }
        }
        return List.copyOf(phrases);
    }

    public static int countTermOccurrences(String normalizedContent, String term) {
        if (normalizedContent.isBlank() || term.isBlank()) {
            return 0;
        }

        Pattern pattern = Pattern.compile(
                "(?<![\\p{L}\\p{M}])" + Pattern.quote(term) + "(?![\\p{L}\\p{M}])"
        );
        Matcher matcher = pattern.matcher(normalizedContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Exact word match, or a short Turkish suffix on the same stem (e.g. namaz → namazın).
     */
    public static boolean termMatches(String normalizedText, String term) {
        if (normalizedText.isBlank() || term.isBlank()) {
            return false;
        }
        if (countTermOccurrences(normalizedText, term) > 0) {
            return true;
        }
        if (term.length() < 4) {
            return false;
        }

        Matcher matcher = TERM.matcher(normalizedText);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith(term) && token.length() - term.length() <= 3) {
                return true;
            }
        }
        return false;
    }

    public static int countPageReferences(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = PAGE_REFERENCE.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static double letterRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        int letters = 0;
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            total++;
            if (Character.isLetter(ch)) {
                letters++;
            }
        }
        return total == 0 ? 0.0 : (double) letters / total;
    }

    public static double digitRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        int digits = 0;
        int total = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            total++;
            if (Character.isDigit(ch)) {
                digits++;
            }
        }
        return total == 0 ? 0.0 : (double) digits / total;
    }

    public static double tableOfContentsLineRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        String[] lines = text.split("\\R");
        int tocLikeLines = 0;
        int nonBlankLines = 0;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            nonBlankLines++;
            String normalizedLine = normalize(line);
            if (normalizedLine.contains("içindekiler")
                    || normalizedLine.contains("fihrist")
                    || line.matches(".*\\.{3,}.*\\d+.*")
                    || line.matches(".*\\s+\\d+\\s*$")) {
                tocLikeLines++;
            }
        }

        return nonBlankLines == 0 ? 0.0 : (double) tocLikeLines / nonBlankLines;
    }

    private static List<String> extractKeywordSequence(String text, int minLength, Set<String> stopWords) {
        List<String> terms = new ArrayList<>();
        Matcher matcher = TERM.matcher(normalize(text));
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() >= minLength && !stopWords.contains(term)) {
                terms.add(term);
            }
        }
        return terms;
    }
}
