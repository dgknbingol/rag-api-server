package com.aislam.rag.dto;

public record DeviceSessionResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        /** iOS: App Attest kaydı başarılı — istemci local flag tutar. */
        boolean appAttestRegistered
) {
    public DeviceSessionResponse(String accessToken, String tokenType, long expiresIn) {
        this(accessToken, tokenType, expiresIn, false);
    }
}
