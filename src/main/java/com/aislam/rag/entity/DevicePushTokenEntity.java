package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "device_push_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_push_device_id", columnNames = "device_id")
)
public class DevicePushTokenEntity {

    @Id
    private UUID id;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** Native FCM (Android) veya APNs (iOS) device token. */
    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Column(nullable = false, length = 16)
    private String platform;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "district_id", length = 32)
    private String districtId;

    @Column(nullable = false, length = 64)
    private String timezone;

    /** prayerId -> { atTime, before, days } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prayer_prefs", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> prayerPrefs;

    /** dailyKind -> { enabled, melodyIndex, hour, minute, days } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "daily_prefs", columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, Object> dailyPrefs = new LinkedHashMap<>();

    /** competitionKind -> { atTime, before, days } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "competition_prefs", columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, Object> competitionPrefs = new LinkedHashMap<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DevicePushTokenEntity() {
    }

    public DevicePushTokenEntity(
            UUID id,
            String deviceId,
            String pushToken,
            String platform,
            double latitude,
            double longitude,
            String districtId,
            String timezone,
            Map<String, Object> prayerPrefs
    ) {
        this.id = id;
        this.deviceId = deviceId;
        this.pushToken = pushToken;
        this.platform = platform;
        this.latitude = latitude;
        this.longitude = longitude;
        this.districtId = districtId;
        this.timezone = timezone;
        this.prayerPrefs = prayerPrefs;
        this.dailyPrefs = new LinkedHashMap<>();
        this.competitionPrefs = new LinkedHashMap<>();
        this.enabled = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Map<String, Object> getPrayerPrefs() {
        return prayerPrefs;
    }

    public void setPrayerPrefs(Map<String, Object> prayerPrefs) {
        this.prayerPrefs = prayerPrefs;
    }

    public Map<String, Object> getDailyPrefs() {
        return dailyPrefs != null ? dailyPrefs : Map.of();
    }

    public void setDailyPrefs(Map<String, Object> dailyPrefs) {
        this.dailyPrefs = dailyPrefs != null ? dailyPrefs : new LinkedHashMap<>();
    }

    public Map<String, Object> getCompetitionPrefs() {
        return competitionPrefs != null ? competitionPrefs : Map.of();
    }

    public void setCompetitionPrefs(Map<String, Object> competitionPrefs) {
        this.competitionPrefs = competitionPrefs != null ? competitionPrefs : new LinkedHashMap<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
