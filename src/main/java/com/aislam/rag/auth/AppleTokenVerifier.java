package com.aislam.rag.auth;

import com.aislam.rag.config.AuthProperties;
import com.aislam.rag.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppleTokenVerifier {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AuthProperties authProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, PublicKey> keysByKid = new ConcurrentHashMap<>();

    public AppleTokenVerifier(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public SocialIdentity verify(String identityToken) {
        if (identityToken == null || identityToken.isBlank()) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        String kid = headerKid(identityToken.trim());
        PublicKey key = keysByKid.computeIfAbsent(kid, ignored -> fetchKey(kid));
        if (key == null) {
            keysByKid.clear();
            key = fetchKey(kid);
        }
        if (key == null) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        Claims claims;
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(APPLE_ISSUER)
                    .requireAudience(authProperties.appleAudience())
                    .build()
                    .parseSignedClaims(identityToken.trim());
            claims = parsed.getPayload();
        } catch (JwtException ex) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }

        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            email = "apple-" + subject + "@privaterelay.appleid.com";
        }
        String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Apple";
        return new SocialIdentity("apple", subject, email.toLowerCase(), name);
    }

    private String headerKid(String token) {
        try {
            String headerPart = token.split("\\.")[0];
            byte[] decoded = Base64.getUrlDecoder().decode(headerPart);
            JsonNode header = objectMapper.readTree(decoded);
            String kid = header.path("kid").asText(null);
            if (kid == null || kid.isBlank()) {
                throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
            }
            return kid;
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }
    }

    private PublicKey fetchKey(String kid) {
        String body;
        try {
            body = restClient.get().uri(APPLE_KEYS_URL).retrieve().body(String.class);
        } catch (RestClientException ex) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }
        try {
            JsonNode keys = objectMapper.readTree(body).path("keys");
            if (!keys.isArray()) {
                return null;
            }
            for (JsonNode keyNode : keys) {
                if (!kid.equals(keyNode.path("kid").asText())) {
                    continue;
                }
                byte[] modulus = Base64.getUrlDecoder().decode(keyNode.path("n").asText());
                byte[] exponent = Base64.getUrlDecoder().decode(keyNode.path("e").asText());
                RSAPublicKeySpec spec = new RSAPublicKeySpec(
                        new BigInteger(1, modulus),
                        new BigInteger(1, exponent)
                );
                PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
                keysByKid.put(kid, publicKey);
                return publicKey;
            }
            return null;
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Apple oturumu doğrulanamadı.", "INVALID_SOCIAL_TOKEN");
        }
    }
}
