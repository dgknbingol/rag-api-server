package com.aislam.rag.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final MediaType JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private static final ExchangeStrategies UTF8_JSON_STRATEGIES = buildUtf8JsonStrategies();

    @Bean
    public WebClient deepSeekWebClient(DeepSeekProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new JdkClientHttpConnector(
                        HttpClient.newBuilder()
                                .connectTimeout(properties.connectTimeout())
                                .build()
                ))
                .exchangeStrategies(UTF8_JSON_STRATEGIES)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    @Bean
    public WebClient lmStudioWebClient(LmStudioProperties properties) {
        return buildWebClient(
                properties.baseUrl(),
                properties.connectTimeout(),
                properties.readTimeout()
        );
    }

    @Bean
    public WebClient qdrantWebClient(QdrantProperties properties) {
        return buildWebClient(
                properties.baseUrl(),
                properties.connectTimeout(),
                properties.readTimeout()
        );
    }

    @Bean
    public WebClient diyanetWebClient(PrayerTimesProperties properties) {
        return buildWebClient(
                properties.diyanetBaseUrl(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30)
        );
    }

    @Bean
    public WebClient nominatimWebClient(PrayerTimesProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.nominatimBaseUrl())
                .clientConnector(new JdkClientHttpConnector(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build()
                ))
                .exchangeStrategies(UTF8_JSON_STRATEGIES)
                .defaultHeader("User-Agent", "AiSLAM/1.0 (prayer-times)")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static ExchangeStrategies buildUtf8JsonStrategies() {
        ObjectMapper objectMapper = new ObjectMapper();

        return ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(
                                    objectMapper,
                                    JSON_UTF8,
                                    MediaType.APPLICATION_JSON
                            )
                    );
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(
                                    objectMapper,
                                    JSON_UTF8,
                                    MediaType.APPLICATION_JSON
                            )
                    );
                })
                .build();
    }

    private static WebClient buildWebClient(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .exchangeStrategies(UTF8_JSON_STRATEGIES)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, ApiMediaTypes.APPLICATION_JSON_UTF8_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                .build();
    }
}
