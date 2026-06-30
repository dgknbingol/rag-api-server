package com.aislam.rag.service;

import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.dto.IndexDocumentResponse;
import com.aislam.rag.dto.UploadDocumentRequest;

public interface DocumentIndexService {

    IndexDocumentResponse index(RagDocument document);

    IndexDocumentResponse index(UploadDocumentRequest request);

    IndexDocumentResponse indexPdf(RagDocument document, byte[] pdfBytes);
}
