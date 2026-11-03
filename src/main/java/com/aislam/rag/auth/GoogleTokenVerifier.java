package com.aislam.rag.auth;

import com.aislam.rag.config.AuthProperties;
import com.aislam.rag.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GoogleTokenVerifier {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final AuthProperties authProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GoogleTokenVerifier(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public SocialIdentity verify(String idToken) {
        List<String> allowedAudiences = authProperties.googleClientIdList();
        if (allowedAudiences.isEmpty()) {
            throw new AuthException("Google ile giriş henüz yapılandırılmadı.", "SOCIAL_LOGIN_UNAVAILABLE");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new AuthException("Google oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        String body;
        try {
            body = restClient.get()
                    .uri(TOKEN_INFO_URL, idToken.trim())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            throw new AuthException("Google oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new AuthException("Google oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        String audience = text(json, "aud");
        if (audience == null || !allowedAudiences.contains(audience)) {
            throw new AuthException("Google oturumu bu uygulamaya ait değil.", "INVALID_SOCIAL_TOKEN");
        }

        String email = text(json, "email");
        String subject = text(json, "sub");
        if (email == null || subject == null) {
            throw new AuthException("Google hesabından e-posta alınamadı.", "INVALID_SOCIAL_TOKEN");
        }
        if (!"true".equalsIgnoreCase(text(json, "email_verified"))) {
            throw new AuthException("Google e-posta adresi doğrulanmamış.", "INVALID_SOCIAL_TOKEN");
        }

        String name = text(json, "name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, Math.max(1, email.indexOf('@')));
        }
        return new SocialIdentity("google", subject, email.toLowerCase(), name.trim());
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
