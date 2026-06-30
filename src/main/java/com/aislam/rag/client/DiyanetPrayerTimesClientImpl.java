package com.aislam.rag.client;

import com.aislam.rag.config.PrayerTimesProperties;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DiyanetPrayerTimesClientImpl implements DiyanetPrayerTimesClient {

    private static final Logger log = LoggerFactory.getLogger(DiyanetPrayerTimesClientImpl.class);
    private static final String ERROR_CODE = "PRAYER_TIMES_ERROR";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DiyanetPrayerTimesClientImpl(
            @Qualifier("diyanetWebClient") WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JsonNode> fetchMonthlyByDistrict(String districtId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        log.info("Diyanet monthly request districtId={} year={} month={}", districtId, year, month);

        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/prayer-times/{districtId}/monthly")
                            .queryParam("startDate", startDate)
                            .build(districtId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseDataArray(json, "Diyanet monthly prayer times");
        } catch (RagException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            log.error("Diyanet connection failed: {}", ex.getMessage(), ex);
            throw new RagException("Diyanet connection failed: " + ex.getMessage(), ERROR_CODE, ex);
        } catch (Exception ex) {
            log.error("Diyanet request failed: {}", ex.getMessage(), ex);
            throw new RagException("Diyanet request failed: " + ex.getMessage(), ERROR_CODE, ex);
        }
    }

    @Override
    public List<JsonNode> fetchStatesByCountry(String countryId) {
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/locations/states")
                            .queryParam("countryId", countryId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseDataArray(json, "Diyanet states");
        } catch (Exception ex) {
            throw new RagException("Diyanet states request failed: " + ex.getMessage(), ERROR_CODE, ex);
        }
    }

    @Override
    public List<JsonNode> fetchDistrictsByState(String stateId) {
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/locations/districts")
                            .queryParam("stateId", stateId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseDataArray(json, "Diyanet districts");
        } catch (Exception ex) {
            throw new RagException("Diyanet districts request failed: " + ex.getMessage(), ERROR_CODE, ex);
        }
    }

    private List<JsonNode> parseDataArray(String json, String label) throws Exception {
        if (json == null || json.isBlank()) {
            throw new RagException(label + " returned an empty response", ERROR_CODE);
        }

        JsonNode root = objectMapper.readTree(json);
        if (!root.path("success").asBoolean(true) || root.path("code").asInt(200) != 200) {
            throw new RagException(label + " error: " + root.path("message").asText(), ERROR_CODE);
        }

        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new RagException(label + " data is empty", ERROR_CODE);
        }

        List<JsonNode> items = new ArrayList<>();
        data.forEach(items::add);
        return List.copyOf(items);
    }
}
