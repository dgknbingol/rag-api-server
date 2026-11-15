package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "remote_config")
public class RemoteConfigEntity {

    @Id
    @Column(length = 64)
    private String configKey;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RemoteConfigEntity() {
    }

    public RemoteConfigEntity(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
