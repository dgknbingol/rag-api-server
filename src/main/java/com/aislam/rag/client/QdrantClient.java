package com.aislam.rag.client;

import com.aislam.rag.domain.DocumentChunkKey;
import com.aislam.rag.domain.QdrantPoint;
import com.aislam.rag.domain.SourceChunk;

import java.util.Collection;
import java.util.List;

public interface QdrantClient {

    void ensureCollectionExists();

    List<SourceChunk> search(float[] vector, int limit);

    List<SourceChunk> findByDocumentChunkKeys(Collection<DocumentChunkKey> keys);

    void upsert(List<QdrantPoint> points);
}
