package com.aislam.rag.config;

import org.springframework.http.MediaType;

public final class ApiMediaTypes {

    public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";
    public static final MediaType APPLICATION_JSON_UTF8 = MediaType.parseMediaType(APPLICATION_JSON_UTF8_VALUE);

    private ApiMediaTypes() {
    }
}
