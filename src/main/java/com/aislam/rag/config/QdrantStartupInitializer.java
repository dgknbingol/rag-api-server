package com.aislam.rag.config;

import com.aislam.rag.client.QdrantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ask.pipeline", havingValue = "rag")
public class QdrantStartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QdrantStartupInitializer.class);

    private final QdrantClient qdrantClient;

    public QdrantStartupInitializer(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        qdrantClient.ensureCollectionExists();
        log.info("Qdrant collection ready");
    }
}
