package com.aislam.rag.dto;

import java.util.List;

/**
 * Flexible content block. Fields depend on {@code type}:
 * heading/paragraph/source -> text
 * bullet -> items
 * youtube -> title, url (optional video; omit block entirely if no video)
 */
public record EducationBlockDto(
        String type,
        String text,
        String title,
        String url,
        List<String> items
) {
}
