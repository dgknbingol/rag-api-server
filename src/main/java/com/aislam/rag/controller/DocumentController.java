package com.aislam.rag.controller;

import com.aislam.rag.config.AdsConfigProperties;
import com.aislam.rag.config.ApiMediaTypes;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.PdfUploadResponse;
import com.aislam.rag.dto.UploadDocumentRequest;
import com.aislam.rag.security.ApiSecretAuth;
import com.aislam.rag.service.DocumentIndexService;
import com.aislam.rag.service.PdfIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/documents", produces = ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
public class DocumentController {

    public static final String ADMIN_KEY_HEADER = "X-Config-Admin-Key";

    private final DocumentIndexService documentIndexService;
    private final PdfIngestionService pdfIngestionService;
    private final AdsConfigProperties adsConfigProperties;

    public DocumentController(
            DocumentIndexService documentIndexService,
            PdfIngestionService pdfIngestionService,
            AdsConfigProperties adsConfigProperties
    ) {
        this.documentIndexService = documentIndexService;
        this.pdfIngestionService = pdfIngestionService;
        this.adsConfigProperties = adsConfigProperties;
    }

    @PostMapping(
            value = "/index",
            consumes = {ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE, "application/json"}
    )
    public ResponseEntity<IndexDocumentResponse> index(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @Valid @RequestBody UploadDocumentRequest request
    ) {
        requireAdmin(adminKey);
        return ResponseEntity.ok(documentIndexService.index(request));
    }

    @PostMapping(value = "/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PdfUploadResponse> uploadPdf(
            @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String adminKey,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "category", required = false) String category
    ) {
        requireAdmin(adminKey);
        return ResponseEntity.ok(pdfIngestionService.ingest(file, title, source, category));
    }

    private void requireAdmin(String adminKey) {
        ApiSecretAuth.requireConfigured(
                adsConfigProperties.isAdminConfigured(),
                "ADS_CONFIG_ADMIN_DISABLED",
                "Documents admin key tanımlı değil (ADS_CONFIG_ADMIN_KEY)."
        );
        ApiSecretAuth.requireMatch(
                adminKey,
                adsConfigProperties.adminKey(),
                "ADS_CONFIG_FORBIDDEN",
                "Geçersiz admin anahtarı."
        );
    }
}
