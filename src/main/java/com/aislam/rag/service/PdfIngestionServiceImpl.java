package com.aislam.rag.service;

import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.PdfUploadResponse;
import com.aislam.rag.exception.RagException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfIngestionServiceImpl implements PdfIngestionService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DocumentIndexService documentIndexService;

    public PdfIngestionServiceImpl(DocumentIndexService documentIndexService) {
        this.documentIndexService = documentIndexService;
    }

    @Override
    public PdfUploadResponse ingest(MultipartFile file, String title, String source, String category) {
        validatePdfFile(file);

        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (Exception ex) {
            throw new RagException("Failed to read uploaded PDF", ex);
        }

        String resolvedTitle = resolveTitle(title, file.getOriginalFilename());
        String resolvedSource = resolveOptional(source, file.getOriginalFilename(), "pdf-upload");
        String resolvedCategory = resolveOptional(category, null, "general");

        RagDocument document = RagDocument.of(resolvedTitle, resolvedSource, "", resolvedCategory);
        IndexDocumentResponse indexResult = documentIndexService.indexPdf(document, pdfBytes);

        if ("NO_CONTENT".equals(indexResult.status())) {
            return PdfUploadResponse.empty(indexResult.documentId(), resolvedTitle);
        }

        return PdfUploadResponse.indexed(
                indexResult.documentId(),
                resolvedTitle,
                indexResult.chunkCount(),
                indexResult.sectionTitleStats()
        );
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RagException("PDF file is required");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean pdfContentType = contentType != null && contentType.equalsIgnoreCase(PDF_CONTENT_TYPE);
        boolean pdfExtension = filename != null && filename.toLowerCase().endsWith(".pdf");

        if (!pdfContentType && !pdfExtension) {
            throw new RagException("Uploaded file must be a PDF");
        }
    }

    private String resolveTitle(String title, String originalFilename) {
        if (title != null && !title.isBlank()) {
            return title.strip();
        }
        if (originalFilename != null && !originalFilename.isBlank()) {
            String name = originalFilename.strip();
            if (name.toLowerCase().endsWith(".pdf")) {
                return name.substring(0, name.length() - 4);
            }
            return name;
        }
        return "Untitled PDF";
    }

    private String resolveOptional(String value, String fallbackFilename, String defaultValue) {
        if (value != null && !value.isBlank()) {
            return value.strip();
        }
        if (fallbackFilename != null && !fallbackFilename.isBlank()) {
            return fallbackFilename.strip();
        }
        return defaultValue;
    }
}
