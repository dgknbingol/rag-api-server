package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/** Ezan dışı (günlük içerik / yarışma) push idempotency. */
@Entity
@Table(
        name = "app_push_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_app_push_delivery",
                columnNames = {"device_id", "event_date", "channel", "item_key", "kind"}
        )
)
public class AppPushDeliveryEntity {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "event_date", nullable = false, length = 10)
    private String eventDate;

    /** daily | competition */
    @Column(nullable = false, length = 32)
    private String channel;

    /** ayet | dua | ... | daily */
    @Column(name = "item_key", nullable = false, length = 32)
    private String itemKey;

    /** fire | atTime | before */
    @Column(nullable = false, length = 16)
    private String kind;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected AppPushDeliveryEntity() {
    }

    public AppPushDeliveryEntity(
            UUID id,
            String deviceId,
            String eventDate,
            String channel,
            String itemKey,
            String kind
    ) {
        this.id = id;
        this.deviceId = deviceId;
        this.eventDate = eventDate;
        this.channel = channel;
        this.itemKey = itemKey;
        this.kind = kind;
    }

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
