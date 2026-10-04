package com.aislam.rag.repository;

import com.aislam.rag.entity.DevicePushTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushTokenEntity, UUID> {

    Optional<DevicePushTokenEntity> findByDeviceId(String deviceId);

    List<DevicePushTokenEntity> findByEnabledTrue();

    void deleteByDeviceId(String deviceId);
}
