package com.aislam.rag.repository;

import com.aislam.rag.entity.RemoteConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemoteConfigRepository extends JpaRepository<RemoteConfigEntity, String> {
}
