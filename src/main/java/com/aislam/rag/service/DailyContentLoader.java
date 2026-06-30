package com.aislam.rag.service;

import com.aislam.rag.dto.DailyItemDto;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DailyContentLoader {

    private static final Logger log = LoggerFactory.getLogger(DailyContentLoader.class);

    private static final String AYET_PATH = "daily/daily_ayet.json";
    private static final String DUA_PATH = "daily/daily_dua.json";
    private static final String HADIS_PATH = "daily/daily_hadis.json";

    private final ObjectMapper objectMapper;
    private final List<DailyItemDto> ayetler;
    private final List<DailyItemDto> dualar;
    private final List<DailyItemDto> hadisler;

    public DailyContentLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.ayetler = load(AYET_PATH);
        this.dualar = load(DUA_PATH);
        this.hadisler = load(HADIS_PATH);
        log.info(
                "Daily content loaded ayet={} dua={} hadis={}",
                ayetler.size(),
                dualar.size(),
                hadisler.size()
        );
    }

    public List<DailyItemDto> ayetler() {
        return ayetler;
    }

    public List<DailyItemDto> dualar() {
        return dualar;
    }

    public List<DailyItemDto> hadisler() {
        return hadisler;
    }

    private List<DailyItemDto> load(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            List<DailyItemDto> items = objectMapper.readValue(input, new TypeReference<>() {});
            if (items == null || items.isEmpty()) {
                throw new RagException("Daily content file is empty: " + path, "DAILY_CONTENT_ERROR");
            }
            return List.copyOf(items);
        } catch (IOException ex) {
            throw new RagException("Failed to load daily content: " + path, "DAILY_CONTENT_ERROR", ex);
        }
    }
}
