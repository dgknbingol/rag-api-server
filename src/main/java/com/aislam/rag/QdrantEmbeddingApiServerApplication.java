package com.aislam.rag;

import com.aislam.rag.config.AuthProperties;
import com.aislam.rag.config.AskProperties;
import com.aislam.rag.config.ChatProperties;
import com.aislam.rag.config.CorsProperties;
import com.aislam.rag.config.DailyProperties;
import com.aislam.rag.config.PrayerTimesProperties;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.config.RevenueCatProperties;
import com.aislam.rag.config.DeepSeekProperties;
import com.aislam.rag.config.LmStudioProperties;
import com.aislam.rag.config.QdrantProperties;
import com.aislam.rag.config.RagProperties;
import com.aislam.rag.config.RerankingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AuthProperties.class,
        LmStudioProperties.class,
        QdrantProperties.class,
        RagProperties.class,
        RerankingProperties.class,
        CorsProperties.class,
        DeepSeekProperties.class,
        AskProperties.class,
        ChatProperties.class,
        DailyProperties.class,
        PrayerTimesProperties.class,
        PushProperties.class,
        RevenueCatProperties.class
})
public class QdrantEmbeddingApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QdrantEmbeddingApiServerApplication.class, args);
    }
}
