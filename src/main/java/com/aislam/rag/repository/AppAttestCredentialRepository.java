package com.aislam.rag.repository;

import com.aislam.rag.entity.AppAttestCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppAttestCredentialRepository extends JpaRepository<AppAttestCredentialEntity, UUID> {
    Optional<AppAttestCredentialEntity> findByKeyId(String keyId);
}
