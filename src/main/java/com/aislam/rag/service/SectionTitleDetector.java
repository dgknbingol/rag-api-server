package com.aislam.rag.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SectionTitleDetector {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MIN_TITLE_LENGTH = 2;
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(?:\\d+[.)]|\\d+\\)|[a-zA-ZçğıöşüÇĞİÖŞÜ][.)]|[IVXLC]+[.)])\\s*\\S.*"
    );

    public boolean isSectionTitle(String line) {
        if (line == null) {
            return false;
        }

        String trimmed = line.strip();
        if (trimmed.length() < MIN_TITLE_LENGTH || trimmed.length() > MAX_TITLE_LENGTH) {
            return false;
        }

        if (NUMBERED_HEADING.matcher(trimmed).matches()) {
            return true;
        }

        return isMostlyUppercase(trimmed);
    }

    private boolean isMostlyUppercase(String text) {
        int letters = 0;
        int uppercaseLetters = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isLetter(ch)) {
                continue;
            }
            letters++;
            char upper = String.valueOf(ch).toUpperCase(TURKISH).charAt(0);
            if (ch == upper) {
                uppercaseLetters++;
            }
        }

        return letters >= MIN_TITLE_LENGTH && ((double) uppercaseLetters / letters) >= 0.85;
    }
}
