package com.aislam.rag.client;

import com.fasterxml.jackson.databind.JsonNode;

public interface NominatimClient {

    JsonNode reverseGeocode(double latitude, double longitude);
}
