package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UploadDocumentRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 500)
        String title,

        @NotBlank(message = "content must not be blank")
        @Size(max = 500_000)
        String content,

        @NotBlank(message = "source must not be blank")
        @Size(max = 500)
        String source,

        @Size(max = 100)
        String category
) {
    public String resolvedCategory() {
        return category == null || category.isBlank() ? "general" : category;
    }
}
