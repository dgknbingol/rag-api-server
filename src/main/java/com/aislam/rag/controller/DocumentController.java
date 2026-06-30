package com.aislam.rag.controller;

import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.PdfUploadResponse;
import com.aislam.rag.dto.UploadDocumentRequest;
import com.aislam.rag.service.DocumentIndexService;
import com.aislam.rag.service.PdfIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/documents", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class DocumentController {

    private final DocumentIndexService documentIndexService;
    private final PdfIngestionService pdfIngestionService;

    public DocumentController(
            DocumentIndexService documentIndexService,
            PdfIngestionService pdfIngestionService
    ) {
        this.documentIndexService = documentIndexService;
        this.pdfIngestionService = pdfIngestionService;
    }

    @PostMapping(
            value = "/index",
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<IndexDocumentResponse> index(@Valid @RequestBody UploadDocumentRequest request) {
        return ResponseEntity.ok(documentIndexService.index(request));
    }

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PdfUploadResponse> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "category", required = false) String category
    ) {
        return ResponseEntity.ok(pdfIngestionService.ingest(file, title, source, category));
    }
}
