package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "app_attest_credentials",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_attest_key", columnNames = "key_id")
)
public class AppAttestCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key_id", nullable = false, length = 256)
    private String keyId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Lob
    @Column(name = "credential_json", nullable = false, columnDefinition = "TEXT")
    private String credentialJson;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppAttestCredentialEntity() {
    }

    public AppAttestCredentialEntity(String keyId, UUID deviceId, String credentialJson, long signCount) {
        this.keyId = keyId;
        this.deviceId = deviceId;
        this.credentialJson = credentialJson;
        this.signCount = signCount;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getKeyId() {
        return keyId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getCredentialJson() {
        return credentialJson;
    }

    public long getSignCount() {
        return signCount;
    }

    public void updateCredential(String credentialJson, long signCount) {
        this.credentialJson = credentialJson;
        this.signCount = signCount;
        this.updatedAt = Instant.now();
    }

    public void bindDevice(UUID deviceId) {
        this.deviceId = deviceId;
        this.updatedAt = Instant.now();
    }
}
