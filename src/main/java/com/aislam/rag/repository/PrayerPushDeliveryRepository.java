package com.aislam.rag.repository;

import com.aislam.rag.entity.PrayerPushDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrayerPushDeliveryRepository extends JpaRepository<PrayerPushDeliveryEntity, UUID> {

    boolean existsByDeviceIdAndPrayerDateAndPrayerIdAndKind(
            String deviceId,
            String prayerDate,
            String prayerId,
            String kind
    );
}
