package com.aislam.rag.service;

import com.aislam.rag.dto.PdfUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PdfIngestionService {

    PdfUploadResponse ingest(MultipartFile file, String title, String source, String category);
}
