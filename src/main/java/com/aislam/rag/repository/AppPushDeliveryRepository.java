package com.aislam.rag.repository;

import com.aislam.rag.entity.AppPushDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppPushDeliveryRepository extends JpaRepository<AppPushDeliveryEntity, UUID> {

    boolean existsByDeviceIdAndEventDateAndChannelAndItemKeyAndKind(
            String deviceId,
            String eventDate,
            String channel,
            String itemKey,
            String kind
    );
}
