package com.aislam.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ParagraphSplitter {

    private static final Pattern PAGE_BREAK = Pattern.compile("\\f+");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n+");
    private static final int SECTION_TITLE_MAX_LENGTH = 120;

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        String withoutPageBreaks = PAGE_BREAK.matcher(normalized).replaceAll("\n\n");

        List<String> paragraphs = new ArrayList<>();
        for (String block : BLANK_LINES.split(withoutPageBreaks)) {
            addParagraphOrTitleBlocks(block.strip(), paragraphs);
        }

        return List.copyOf(paragraphs);
    }

    private void addParagraphOrTitleBlocks(String block, List<String> paragraphs) {
        if (block.isBlank()) {
            return;
        }

        String[] lines = block.split("\\n");
        if (lines.length == 1) {
            paragraphs.add(block);
            return;
        }

        StringBuilder body = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }

            if (isSectionTitle(line) && !body.isEmpty()) {
                paragraphs.add(body.toString().strip());
                body.setLength(0);
                paragraphs.add(line);
            } else if (isSectionTitle(line) && body.isEmpty()) {
                paragraphs.add(line);
            } else {
                if (!body.isEmpty()) {
                    body.append('\n');
                }
                body.append(line);
            }
        }

        if (!body.isEmpty()) {
            paragraphs.add(body.toString().strip());
        }
    }

    private boolean isSectionTitle(String line) {
        if (line.length() > SECTION_TITLE_MAX_LENGTH) {
            return false;
        }
        if (line.endsWith(".") || line.endsWith("!") || line.endsWith("?")) {
            return false;
        }
        if (line.chars().filter(ch -> ch == '.').count() > 1) {
            return false;
        }
        return true;
    }
}
