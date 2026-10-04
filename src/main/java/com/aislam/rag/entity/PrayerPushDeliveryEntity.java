package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "prayer_push_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_prayer_push_delivery",
                columnNames = {"device_id", "prayer_date", "prayer_id", "kind"}
        )
)
public class PrayerPushDeliveryEntity {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "prayer_date", nullable = false, length = 10)
    private String prayerDate;

    @Column(name = "prayer_id", nullable = false, length = 16)
    private String prayerId;

    /** atTime | before */
    @Column(nullable = false, length = 16)
    private String kind;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected PrayerPushDeliveryEntity() {
    }

    public PrayerPushDeliveryEntity(
            UUID id,
            String deviceId,
            String prayerDate,
            String prayerId,
            String kind
    ) {
        this.id = id;
        this.deviceId = deviceId;
        this.prayerDate = prayerDate;
        this.prayerId = prayerId;
        this.kind = kind;
    }

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPrayerDate() {
        return prayerDate;
    }

    public String getPrayerId() {
        return prayerId;
    }

    public String getKind() {
        return kind;
    }
}
