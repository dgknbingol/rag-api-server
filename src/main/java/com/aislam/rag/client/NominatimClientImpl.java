package com.aislam.rag.client;

import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
public class NominatimClientImpl implements NominatimClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimClientImpl.class);
    private static final String ERROR_CODE = "PRAYER_TIMES_ERROR";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public NominatimClientImpl(
            @Qualifier("nominatimWebClient") WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode reverseGeocode(double latitude, double longitude) {
        log.info("Nominatim reverse geocode lat={} lon={}", latitude, longitude);

        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "json")
                            .queryParam("accept-language", "tr")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isBlank()) {
                throw new RagException("Nominatim returned an empty response", ERROR_CODE);
            }

            JsonNode root = objectMapper.readTree(json);
            if (root.path("error").isTextual()) {
                throw new RagException("Nominatim error: " + root.path("error").asText(), ERROR_CODE);
            }

            return root;
        } catch (RagException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            log.error("Nominatim connection failed: {}", ex.getMessage(), ex);
            throw new RagException("Nominatim connection failed: " + ex.getMessage(), ERROR_CODE, ex);
        } catch (Exception ex) {
            log.error("Nominatim request failed: {}", ex.getMessage(), ex);
            throw new RagException("Nominatim request failed: " + ex.getMessage(), ERROR_CODE, ex);
        }
    }
}
