package com.aislam.rag.service;

import com.aislam.rag.domain.PdfPageText;
import com.aislam.rag.domain.RagDocument;
import com.aislam.rag.domain.TextChunk;

import java.util.List;

public interface ChunkingService {

    List<TextChunk> chunk(String text);

    List<TextChunk> chunk(RagDocument document);

    List<TextChunk> chunkPages(List<PdfPageText> pages);
}
