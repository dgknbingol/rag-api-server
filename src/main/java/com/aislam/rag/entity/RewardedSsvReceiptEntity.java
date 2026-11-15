package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "rewarded_ssv_receipts",
        uniqueConstraints = @UniqueConstraint(name = "uk_rewarded_ssv_tx", columnNames = "transaction_id")
)
public class RewardedSsvReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, length = 128)
    private String transactionId;

    @Column(name = "app_user_id", nullable = false)
    private UUID appUserId;

    @Column(name = "ad_unit", length = 128)
    private String adUnit;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RewardedSsvReceiptEntity() {
    }

    public RewardedSsvReceiptEntity(String transactionId, UUID appUserId, String adUnit) {
        this.transactionId = transactionId;
        this.appUserId = appUserId;
        this.adUnit = adUnit;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public UUID getAppUserId() {
        return appUserId;
    }
}
