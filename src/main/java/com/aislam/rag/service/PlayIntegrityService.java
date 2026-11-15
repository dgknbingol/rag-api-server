package com.aislam.rag.service;

import com.aislam.rag.config.PlayIntegrityProperties;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Iterator;

/**
 * Play Integrity token decode + verdict kontrolü.
 * Credentials: PLAY_INTEGRITY_CREDENTIALS_JSON veya FIREBASE_CREDENTIALS_JSON.
 */
@Service
public class PlayIntegrityService {

    private static final Logger log = LoggerFactory.getLogger(PlayIntegrityService.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/playintegrity";

    private final PlayIntegrityProperties properties;
    private final PushProperties pushProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PlayIntegrityService(
            PlayIntegrityProperties properties,
            PushProperties pushProperties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.pushProperties = pushProperties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public void verifyAndroid(String integrityToken, String expectedRequestHash) {
        if (!properties.enabled()) {
            return;
        }
        if (integrityToken == null || integrityToken.isBlank()) {
            throw new RagException("Play Integrity token gerekli.", "PLAY_INTEGRITY_REQUIRED");
        }

        String credJson = properties.resolveCredentialsJson(pushProperties.firebaseCredentialsJson());
        if (credJson.isBlank()) {
            throw new RagException(
                    "Play Integrity credentials yok (FIREBASE_CREDENTIALS_JSON).",
                    "PLAY_INTEGRITY_NOT_CONFIGURED"
            );
        }

        JsonNode payload = decodeToken(credJson, integrityToken.trim());
        validateVerdict(payload, expectedRequestHash);
    }

    private JsonNode decodeToken(String credentialsJson, String integrityToken) {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(SCOPE);
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            String packageName = properties.packageName();
            String url = "https://playintegrity.googleapis.com/v1/"
                    + packageName
                    + ":decodeIntegrityToken";
            String body = objectMapper.createObjectNode()
                    .put("integrity_token", integrityToken)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Play Integrity decode failed status={} body={}", response.statusCode(), response.body());
                throw new RagException("Play Integrity doğrulanamadı.", "PLAY_INTEGRITY_DECODE_FAILED");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode external = root.path("tokenPayloadExternal");
            if (external.isMissingNode() || external.isNull()) {
                throw new RagException("Play Integrity payload eksik.", "PLAY_INTEGRITY_INVALID");
            }
            return external;
        } catch (RagException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Play Integrity verify error", ex);
            throw new RagException("Play Integrity doğrulanamadı.", "PLAY_INTEGRITY_DECODE_FAILED");
        }
    }

    private void validateVerdict(JsonNode payload, String expectedRequestHash) {
        // Standard API: requestDetails.requestHash ; Classic: requestDetails.nonce
        String requestHash = text(payload.path("requestDetails").path("requestHash"));
        String nonce = text(payload.path("requestDetails").path("nonce"));
        String matched = !requestHash.isBlank() ? requestHash : nonce;
        if (expectedRequestHash != null && !expectedRequestHash.isBlank()) {
            if (!expectedRequestHash.equals(matched)) {
                throw new RagException("Play Integrity challenge eşleşmiyor.", "PLAY_INTEGRITY_HASH_MISMATCH");
            }
        }

        String packageName = text(payload.path("appIntegrity").path("packageName"));
        if (!properties.packageName().equals(packageName)) {
            throw new RagException("Uygulama paketi doğrulanamadı.", "PLAY_INTEGRITY_PACKAGE");
        }

        String recognition = text(payload.path("appIntegrity").path("appRecognitionVerdict"));
        if (properties.requireRecognized() && !"PLAY_RECOGNIZED".equals(recognition)) {
            throw new RagException(
                    "Uygulama Play tarafından tanınmıyor (" + recognition + ").",
                    "PLAY_INTEGRITY_NOT_RECOGNIZED"
            );
        }

        if (properties.requireDeviceIntegrity()) {
            JsonNode verdicts = payload.path("deviceIntegrity").path("deviceRecognitionVerdict");
            boolean ok = false;
            if (verdicts.isArray()) {
                for (Iterator<JsonNode> it = verdicts.elements(); it.hasNext(); ) {
                    String v = it.next().asText("");
                    if ("MEETS_DEVICE_INTEGRITY".equals(v)
                            || "MEETS_STRONG_INTEGRITY".equals(v)
                            || "MEETS_BASIC_INTEGRITY".equals(v)) {
                        ok = true;
                        break;
                    }
                }
            }
            if (!ok) {
                throw new RagException("Cihaz bütünlüğü doğrulanamadı.", "PLAY_INTEGRITY_DEVICE");
            }
        }
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }
}
