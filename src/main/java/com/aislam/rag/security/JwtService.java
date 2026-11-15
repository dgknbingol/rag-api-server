package com.aislam.rag.security;

import com.aislam.rag.config.AuthProperties;
import com.aislam.rag.exception.RagException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String CLAIM_TYP = "typ";
    public static final String TYP_USER = "user";
    public static final String TYP_DEVICE = "device";

    private final SecretKey secretKey;
    private final long expirationSeconds;
    private final long deviceExpirationSeconds;

    public JwtService(AuthProperties authProperties) {
        this.secretKey = Keys.hmacShaKeyFor(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = authProperties.jwtExpirationHours() * 3600L;
        long deviceHours = authProperties.deviceJwtExpirationHours();
        if (deviceHours <= 0) {
            deviceHours = 168;
        }
        this.deviceExpirationSeconds = deviceHours * 3600L;
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYP, TYP_USER)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /** Mobil cihaz oturumu — APK’da secret yok; sunucu imzalı kısa ömürlü token. */
    public String generateDeviceToken(UUID deviceId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(deviceExpirationSeconds);

        return Jwts.builder()
                .subject(deviceId.toString())
                .claim(CLAIM_TYP, TYP_DEVICE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public UUID parseUserId(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TYP, String.class);
        if (typ != null && !TYP_USER.equals(typ)) {
            throw new RagException("Geçersiz kullanıcı tokenı.", "AUTH_TOKEN_INVALID");
        }
        return UUID.fromString(claims.getSubject());
    }

    public UUID parseDeviceId(String token) {
        Claims claims = parseClaims(token);
        if (!TYP_DEVICE.equals(claims.get(CLAIM_TYP, String.class))) {
            throw new RagException("Geçersiz cihaz tokenı.", "DEVICE_TOKEN_INVALID");
        }
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new RagException("Geçersiz cihaz tokenı.", "DEVICE_TOKEN_INVALID");
        }
    }

    public Instant deviceTokenExpiry() {
        return Instant.now().plusSeconds(deviceExpirationSeconds);
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    public long deviceExpirationSeconds() {
        return deviceExpirationSeconds;
    }

    Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
