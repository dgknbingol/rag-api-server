package com.aislam.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceSessionRequest(
        @NotBlank String deviceId,
        /** Play Integrity token (Android). */
        String integrityToken,
        /** /device-challenge’dan gelen tek kullanımlık challenge. */
        String challenge,
        /** android | ios | web */
        String platform,
        /** App Attest keyId (iOS). */
        String appAttestKeyId,
        /** İlk kayıt: base64 attestation object. */
        String appAttestAttestation,
        /** Sonraki oturumlar: base64 assertion. */
        String appAttestAssertion
) {
}
