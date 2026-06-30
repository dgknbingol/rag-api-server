package com.aislam.rag.service;

import com.aislam.rag.client.DiyanetPrayerTimesClient;
import com.aislam.rag.client.NominatimClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DistrictResolverService {

    private static final Logger log = LoggerFactory.getLogger(DistrictResolverService.class);
    private static final String TURKEY_COUNTRY_ID = "2";
    private static final String DEFAULT_DISTRICT_ID = "9541";

    private final NominatimClient nominatimClient;
    private final DiyanetPrayerTimesClient diyanetClient;
    private final Map<String, List<JsonNode>> districtsByStateCache = new ConcurrentHashMap<>();
    private final Map<String, List<JsonNode>> statesByCountryCache = new ConcurrentHashMap<>();
    private final Map<Long, String> coordinateDistrictCache = new ConcurrentHashMap<>();

    public DistrictResolverService(
            NominatimClient nominatimClient,
            DiyanetPrayerTimesClient diyanetClient
    ) {
        this.nominatimClient = nominatimClient;
        this.diyanetClient = diyanetClient;
    }

    public String resolveDistrictId(double latitude, double longitude) {
        long cacheKey = coordinateKey(latitude, longitude);
        String cached = coordinateDistrictCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        JsonNode geocode = nominatimClient.reverseGeocode(latitude, longitude);
        JsonNode address = geocode.path("address");

        String countryCode = address.path("country_code").asText("tr");
        String province = firstNonBlank(
                address.path("province").asText(null),
                address.path("state").asText(null),
                address.path("city").asText(null)
        );
        String locality = firstNonBlank(
                address.path("town").asText(null),
                address.path("city_district").asText(null),
                address.path("county").asText(null),
                address.path("suburb").asText(null),
                address.path("city").asText(null)
        );

        String districtId = resolveDistrictId(countryCode, province, locality);
        coordinateDistrictCache.put(cacheKey, districtId);
        log.info(
                "Resolved districtId={} for lat={} lon={} province={} locality={}",
                districtId,
                latitude,
                longitude,
                province,
                locality
        );
        return districtId;
    }

    private String resolveDistrictId(String countryCode, String province, String locality) {
        if (!"tr".equalsIgnoreCase(countryCode)) {
            return DEFAULT_DISTRICT_ID;
        }

        List<JsonNode> states = statesByCountryCache.computeIfAbsent(
                TURKEY_COUNTRY_ID,
                diyanetClient::fetchStatesByCountry
        );

        String stateId = findStateId(states, province);
        if (stateId == null) {
            log.warn("State not found for province={}, using default district", province);
            return DEFAULT_DISTRICT_ID;
        }

        List<JsonNode> districts = districtsByStateCache.computeIfAbsent(
                stateId,
                diyanetClient::fetchDistrictsByState
        );

        String districtId = matchDistrict(districts, locality);
        if (districtId != null) {
            return districtId;
        }

        districtId = matchDistrict(districts, province);
        if (districtId != null) {
            return districtId;
        }

        return districts.isEmpty() ? DEFAULT_DISTRICT_ID : districts.getFirst().path("_id").asText(DEFAULT_DISTRICT_ID);
    }

    private String findStateId(List<JsonNode> states, String province) {
        String normalizedProvince = normalize(province);
        for (JsonNode state : states) {
            if (matches(normalizedProvince, state.path("name").asText(""))
                    || matches(normalizedProvince, state.path("name_en").asText(""))) {
                return state.path("_id").asText(null);
            }
        }
        return null;
    }

    private String matchDistrict(List<JsonNode> districts, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String normalizedName = normalize(name);
        for (JsonNode district : districts) {
            if (matches(normalizedName, district.path("name").asText(""))
                    || matches(normalizedName, district.path("name_en").asText(""))) {
                return district.path("_id").asText(null);
            }
        }

        for (JsonNode district : districts) {
            String districtName = normalize(district.path("name").asText(""));
            if (districtName.contains(normalizedName) || normalizedName.contains(districtName)) {
                return district.path("_id").asText(null);
            }
        }

        return null;
    }

    private static boolean matches(String left, String right) {
        if (left.isBlank() || right.isBlank()) {
            return false;
        }
        return normalize(left).equals(normalize(right));
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        return normalized.replace('İ', 'I').replace(" ", "");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static long coordinateKey(double latitude, double longitude) {
        long lat = Math.round(latitude * 1000.0);
        long lon = Math.round(longitude * 1000.0);
        return (lat << 32) ^ (lon & 0xffffffffL);
    }
}
