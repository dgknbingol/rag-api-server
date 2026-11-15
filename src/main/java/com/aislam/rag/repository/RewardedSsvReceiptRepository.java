package com.aislam.rag.repository;

import com.aislam.rag.entity.RewardedSsvReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RewardedSsvReceiptRepository extends JpaRepository<RewardedSsvReceiptEntity, UUID> {
    boolean existsByTransactionId(String transactionId);
}
