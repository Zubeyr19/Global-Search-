package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "serial_number")
        },
        indexes = {
                @Index(name = "idx_sensor_zone", columnList = "zone_id"),
                @Index(name = "idx_sensor_type", columnList = "sensor_type"),
                @Index(name = "idx_sensor_status", columnList = "status")
        })
@EntityListeners(com.globalsearch.service.EntitySyncListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "zone")
@EqualsAndHashCode(exclude = "zone")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "serial_number", nullable = false, unique = true, length = 50)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;

    @Column(length = 50)
    private String manufacturer;

    @Column(length = 50)
    private String model;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SensorStatus status = SensorStatus.ACTIVE;

    @Column(name = "last_reading_time")
    private LocalDateTime lastReadingTime;

    @Column(name = "last_reading_value")
    private Double lastReadingValue;

    @Column(name = "unit_of_measurement", length = 20)
    private String unitOfMeasurement;

    @Column(name = "reading_interval")
    private Integer readingInterval; // in seconds

    @Column(name = "alert_threshold_min")
    private Double alertThresholdMin;

    @Column(name = "alert_threshold_max")
    private Double alertThresholdMax;

    @Column(name = "battery_level")
    private Integer batteryLevel; // percentage

    @Column(name = "installation_date")
    private LocalDateTime installationDate;

    @Column(name = "last_maintenance_date")
    private LocalDateTime lastMaintenanceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (installationDate == null) {
            installationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SensorType {
        TEMPERATURE,
        HUMIDITY,
        PRESSURE,
        MOTION,
        LIGHT,
        SMOKE,
        DOOR_WINDOW,
        WATER_LEAK,
        VIBRATION,
        POWER_METER,
        AIR_QUALITY,
        CUSTOM
    }

    public enum SensorStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        FAULTY,
        OFFLINE
    }

    // Helper methods
    public boolean isActive() {
        return status == SensorStatus.ACTIVE;
    }

    public boolean isOnline() {
        if (lastReadingTime == null || readingInterval == null) return false;
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(readingInterval * 3L);
        return lastReadingTime.isAfter(threshold);
    }

    public boolean needsMaintenance() {
        return status == SensorStatus.MAINTENANCE;
    }

    public boolean hasLowBattery() {
        return batteryLevel != null && batteryLevel < 20;
    }

    public boolean isReadingInRange(Double value) {
        if (value == null) return true;
        if (alertThresholdMin != null && value < alertThresholdMin) return false;
        if (alertThresholdMax != null && value > alertThresholdMax) return false;
        return true;
    }
}