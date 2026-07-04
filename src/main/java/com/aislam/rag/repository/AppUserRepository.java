package com.aislam.rag.repository;

import com.aislam.rag.entity.AppUserEntity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {
}
