package com.aislam.rag.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface DiyanetPrayerTimesClient {

    List<JsonNode> fetchMonthlyByDistrict(String districtId, int year, int month);

    List<JsonNode> fetchStatesByCountry(String countryId);

    List<JsonNode> fetchDistrictsByState(String stateId);
}
