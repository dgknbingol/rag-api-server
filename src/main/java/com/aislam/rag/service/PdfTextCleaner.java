package com.aislam.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PdfTextCleaner {

    private static final Pattern HYPHENATED_LINE_BREAK = Pattern.compile("(?<=[\\p{L}])-\\s*\n\\s*(?=\\p{L})");
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[ \\t\\f\\r\\u00A0]+");
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final double REPEAT_THRESHOLD = 0.6;
    private static final int MAX_HEADER_FOOTER_LENGTH = 120;

    public String clean(String rawText) {
        return clean(rawText, Set.of());
    }

    public String clean(String rawText, Set<String> repeatedHeaders) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = normalizeLineEndings(rawText);
        String withoutHeaders = removeRepeatedHeaderLines(normalized, repeatedHeaders);
        String fixedHyphens = fixHyphenatedLineBreaks(withoutHeaders);
        String mergedParagraphs = mergeWrappedLinesPreservingParagraphs(fixedHyphens);
        return collapseWhitespace(mergedParagraphs).trim();
    }

    public Set<String> detectRepeatedHeaders(List<String> pageTexts) {
        if (pageTexts == null || pageTexts.size() < 2) {
            return Set.of();
        }

        Map<String, Integer> firstLineCounts = new HashMap<>();
        int pagesWithContent = 0;

        for (String pageText : pageTexts) {
            List<String> lines = nonBlankLines(pageText);
            if (lines.isEmpty()) {
                continue;
            }

            pagesWithContent++;
            String firstLine = normalizeHeaderKey(lines.getFirst());
            if (isCandidateHeaderFooter(firstLine)) {
                firstLineCounts.merge(firstLine, 1, Integer::sum);
            }
        }

        if (pagesWithContent == 0) {
            return Set.of();
        }

        Set<String> repeated = new HashSet<>();
        int threshold = (int) Math.ceil(pagesWithContent * REPEAT_THRESHOLD);
        firstLineCounts.forEach((line, count) -> {
            if (count >= threshold) {
                repeated.add(line);
            }
        });
        return Set.copyOf(repeated);
    }

    private String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String fixHyphenatedLineBreaks(String text) {
        return HYPHENATED_LINE_BREAK.matcher(text).replaceAll("");
    }

    private String mergeWrappedLinesPreservingParagraphs(String text) {
        String[] paragraphs = PARAGRAPH_BREAK.split(text.strip());
        if (paragraphs.length == 0) {
            return mergeLinesWithinParagraph(text.strip());
        }

        StringBuilder result = new StringBuilder();
        for (String paragraph : paragraphs) {
            String merged = mergeLinesWithinParagraph(paragraph);
            if (merged.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append("\n\n");
            }
            result.append(merged);
        }
        return result.toString();
    }

    private String mergeLinesWithinParagraph(String paragraph) {
        String[] lines = paragraph.split("\\n");
        StringBuilder merged = new StringBuilder();

        for (String rawLine : lines) {
            String line = HORIZONTAL_WHITESPACE.matcher(rawLine.strip()).replaceAll(" ").strip();
            if (line.isEmpty()) {
                continue;
            }
            if (merged.isEmpty()) {
                merged.append(line);
            } else {
                merged.append(' ').append(line);
            }
        }

        return merged.toString();
    }

    private String removeRepeatedHeaderLines(String text, Set<String> repeatedHeaders) {
        if (repeatedHeaders.isEmpty()) {
            return text;
        }

        StringBuilder cleaned = new StringBuilder();
        for (String line : text.split("\\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                cleaned.append('\n');
                continue;
            }
            if (!matchesRepeatedHeader(trimmed, repeatedHeaders)) {
                cleaned.append(trimmed).append('\n');
            }
        }
        return cleaned.toString();
    }

    private boolean matchesRepeatedHeader(String line, Set<String> repeatedHeaders) {
        String key = normalizeHeaderKey(line);
        for (String header : repeatedHeaders) {
            if (header.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeaderKey(String line) {
        return HORIZONTAL_WHITESPACE.matcher(line.strip()).replaceAll(" ");
    }

    private List<String> nonBlankLines(String page) {
        List<String> lines = new ArrayList<>();
        for (String line : page.split("\\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private boolean isCandidateHeaderFooter(String line) {
        return !line.isBlank() && line.length() <= MAX_HEADER_FOOTER_LENGTH;
    }

    private String collapseWhitespace(String text) {
        String[] paragraphs = PARAGRAPH_BREAK.split(text.strip());
        if (paragraphs.length == 0) {
            return collapseLine(text);
        }

        StringBuilder result = new StringBuilder();
        for (String paragraph : paragraphs) {
            String collapsed = collapseLine(paragraph);
            if (collapsed.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append("\n\n");
            }
            result.append(collapsed);
        }

        return MULTIPLE_NEWLINES.matcher(result.toString()).replaceAll("\n\n");
    }

    private String collapseLine(String text) {
        return HORIZONTAL_WHITESPACE.matcher(text.strip()).replaceAll(" ").strip();
    }
}
