package com.aislam.rag.util;

import java.util.Locale;
import java.util.UUID;

public final class QuizPlayerPresentation {

    private static final String[] ACCENTS = {
            "#C9A227", "#D4DCE8", "#B8860B", "#4A7BB7", "#5E8F6E",
            "#8B6FA8", "#A67C52", "#6B8CAE", "#7A6B5D", "#557A99",
            "#032A55", "#6E7F8C", "#8C6E7F", "#5F7A6E", "#9A7B4F",
    };

    private QuizPlayerPresentation() {
    }

    public static String initials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "??";
        }

        String[] parts = displayName.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        String single = parts[0];
        if (single.length() >= 2) {
            return single.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return single.toUpperCase(Locale.ROOT);
    }

    public static String accent(UUID playerId) {
        int index = Math.floorMod(playerId.hashCode(), ACCENTS.length);
        return ACCENTS[index];
    }
}
