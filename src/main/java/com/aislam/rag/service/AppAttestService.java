package com.aislam.rag.service;

import com.aislam.rag.config.AppAttestProperties;
import com.aislam.rag.config.PushProperties;
import com.aislam.rag.dto.DeviceSessionRequest;
import com.aislam.rag.entity.AppAttestCredentialEntity;
import com.aislam.rag.exception.RagException;
import com.aislam.rag.repository.AppAttestCredentialRepository;
import com.webauthn4j.appattest.DeviceCheckManager;
import com.webauthn4j.appattest.authenticator.DCAppleDeviceImpl;
import com.webauthn4j.appattest.data.DCAssertionData;
import com.webauthn4j.appattest.data.DCAssertionParameters;
import com.webauthn4j.appattest.data.DCAssertionRequest;
import com.webauthn4j.appattest.data.DCAttestationData;
import com.webauthn4j.appattest.data.DCAttestationParameters;
import com.webauthn4j.appattest.data.DCAttestationRequest;
import com.webauthn4j.appattest.server.DCServerProperty;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.util.MessageDigestUtil;
import com.webauthn4j.verifier.exception.VerificationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
public class AppAttestService {

    private static final Logger log = LoggerFactory.getLogger(AppAttestService.class);

    private final AppAttestProperties properties;
    private final PushProperties pushProperties;
    private final AppAttestCredentialRepository credentialRepository;

    private DeviceCheckManager deviceCheckManager;
    private ObjectConverter objectConverter;

    public AppAttestService(
            AppAttestProperties properties,
            PushProperties pushProperties,
            AppAttestCredentialRepository credentialRepository
    ) {
        this.properties = properties;
        this.pushProperties = pushProperties;
        this.credentialRepository = credentialRepository;
    }

    @PostConstruct
    void init() {
        objectConverter = DeviceCheckManager.createObjectConverter();
        deviceCheckManager = DeviceCheckManager.createNonStrictDeviceCheckManager(objectConverter);
        if (!properties.production()) {
            deviceCheckManager.getAttestationDataValidator().setProduction(false);
            log.info("App Attest accepts development attestations");
        }
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    @Transactional
    public boolean verifyIos(UUID deviceId, DeviceSessionRequest request) {
        if (!properties.enabled()) {
            return false;
        }
        if (resolveTeamId().isBlank()) {
            throw new RagException(
                    "App Attest teamId yok (APPLE_TEAM_ID / APNS_TEAM_ID).",
                    "APP_ATTEST_NOT_CONFIGURED"
            );
        }

        String keyIdStr = request.appAttestKeyId();
        if (keyIdStr == null || keyIdStr.isBlank()) {
            throw new RagException("App Attest keyId gerekli.", "APP_ATTEST_REQUIRED");
        }
        String challenge = request.challenge();
        if (challenge == null || challenge.isBlank()) {
            throw new RagException("Challenge gerekli.", "DEVICE_CHALLENGE_REQUIRED");
        }

        byte[] keyId = decodeBase64(keyIdStr.trim());
        byte[] challengeBytes = challenge.trim().getBytes(StandardCharsets.UTF_8);
        byte[] clientDataHash = MessageDigestUtil.createSHA256().digest(challengeBytes);

        String attestation = request.appAttestAttestation();
        String assertion = request.appAttestAssertion();

        if (attestation != null && !attestation.isBlank()) {
            registerAttestation(deviceId, keyIdStr.trim(), keyId, challengeBytes, clientDataHash, attestation.trim());
            return true;
        }
        if (assertion != null && !assertion.isBlank()) {
            verifyAssertion(deviceId, keyIdStr.trim(), keyId, challengeBytes, clientDataHash, assertion.trim());
            return true;
        }
        throw new RagException("App Attest attestation veya assertion gerekli.", "APP_ATTEST_REQUIRED");
    }

    private void registerAttestation(
            UUID deviceId,
            String keyIdStr,
            byte[] keyId,
            byte[] challengeBytes,
            byte[] clientDataHash,
            String attestationB64
    ) {
        try {
            DCServerProperty serverProperty = new DCServerProperty(
                    resolveTeamId(),
                    properties.bundleId(),
                    new DefaultChallenge(challengeBytes)
            );
            DCAttestationRequest attestationRequest = new DCAttestationRequest(
                    keyId,
                    decodeBase64(attestationB64),
                    clientDataHash
            );
            DCAttestationData data = deviceCheckManager.validate(
                    attestationRequest,
                    new DCAttestationParameters(serverProperty)
            );

            long signCount = data.getAttestationObject().getAuthenticatorData().getSignCount();
            DCAppleDeviceImpl device = new DCAppleDeviceImpl(
                    data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData(),
                    data.getAttestationObject().getAttestationStatement(),
                    signCount,
                    data.getAttestationObject().getAuthenticatorData().getExtensions()
            );
            String json = objectConverter.getJsonConverter().writeValueAsString(device);

            credentialRepository.findByKeyId(keyIdStr).ifPresentOrElse(
                    existing -> {
                        existing.bindDevice(deviceId);
                        existing.updateCredential(json, signCount);
                        credentialRepository.save(existing);
                    },
                    () -> credentialRepository.save(
                            new AppAttestCredentialEntity(keyIdStr, deviceId, json, signCount)
                    )
            );
            log.info("App Attest registered keyId={} deviceId={}", keyIdStr, deviceId);
        } catch (RagException ex) {
            throw ex;
        } catch (VerificationException ex) {
            log.warn("App Attest attestation failed: {}", ex.getMessage());
            throw new RagException("App Attest doğrulanamadı.", "APP_ATTEST_INVALID");
        } catch (Exception ex) {
            log.warn("App Attest attestation error", ex);
            throw new RagException("App Attest doğrulanamadı.", "APP_ATTEST_INVALID");
        }
    }

    private void verifyAssertion(
            UUID deviceId,
            String keyIdStr,
            byte[] keyId,
            byte[] challengeBytes,
            byte[] clientDataHash,
            String assertionB64
    ) {
        AppAttestCredentialEntity entity = credentialRepository.findByKeyId(keyIdStr)
                .orElseThrow(() -> new RagException(
                        "App Attest anahtarı kayıtlı değil; yeniden attest edin.",
                        "APP_ATTEST_UNKNOWN_KEY"
                ));

        if (!entity.getDeviceId().equals(deviceId)) {
            throw new RagException("App Attest cihazı eşleşmiyor.", "APP_ATTEST_DEVICE_MISMATCH");
        }

        try {
            DCAppleDeviceImpl stored = objectConverter.getJsonConverter()
                    .readValue(entity.getCredentialJson(), DCAppleDeviceImpl.class);

            DCServerProperty serverProperty = new DCServerProperty(
                    resolveTeamId(),
                    properties.bundleId(),
                    new DefaultChallenge(challengeBytes)
            );
            DCAssertionRequest assertionRequest = new DCAssertionRequest(
                    keyId,
                    decodeBase64(assertionB64),
                    clientDataHash
            );
            DCAssertionData data = deviceCheckManager.validate(
                    assertionRequest,
                    new DCAssertionParameters(serverProperty, stored)
            );

            long newCount = data.getAuthenticatorData().getSignCount();
            stored.setCounter(newCount);
            String json = objectConverter.getJsonConverter().writeValueAsString(stored);
            entity.updateCredential(json, newCount);
            credentialRepository.save(entity);
        } catch (RagException ex) {
            throw ex;
        } catch (VerificationException ex) {
            log.warn("App Attest assertion failed: {}", ex.getMessage());
            throw new RagException("App Attest assertion geçersiz.", "APP_ATTEST_INVALID");
        } catch (Exception ex) {
            log.warn("App Attest assertion error", ex);
            throw new RagException("App Attest assertion geçersiz.", "APP_ATTEST_INVALID");
        }
    }

    private String resolveTeamId() {
        if (properties.teamConfigured()) {
            return properties.teamId().trim();
        }
        String fromPush = pushProperties.apnsTeamId();
        return fromPush == null ? "" : fromPush.trim();
    }

    private static byte[] decodeBase64(String value) {
        String v = value.trim();
        try {
            return Base64.getDecoder().decode(v);
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        try {
            return Base64.getUrlDecoder().decode(v);
        } catch (IllegalArgumentException ex) {
            throw new RagException("Geçersiz App Attest base64.", "APP_ATTEST_INVALID");
        }
    }
}
