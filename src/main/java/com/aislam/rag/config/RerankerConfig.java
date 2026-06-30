package com.aislam.rag.config;

import com.aislam.rag.client.CrossEncoderRerankerClient;
import com.aislam.rag.client.RerankerClient;
import com.aislam.rag.service.KeywordReranker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RerankerConfig {

    @Bean
    RerankerClient rerankerClient(RagProperties properties, KeywordReranker keywordReranker) {
        if ("cross-encoder".equalsIgnoreCase(properties.rerankerType())) {
            return new CrossEncoderRerankerClient();
        }
        return keywordReranker::rerank;
    }
}
