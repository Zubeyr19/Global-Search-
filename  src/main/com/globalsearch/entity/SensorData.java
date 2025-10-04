package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data",
        indexes = {
                @Index(name = "idx_sensor_data_sensor_id", columnList = "sensor_id"),
                @Index(name = "idx_sensor_data_timestamp", columnList = "timestamp"),
                @Index(name = "idx_sensor_data_tenant", columnList = "tenant_id"),
                @Index(name = "idx_sensor_data_composite", columnList = "sensor_id, timestamp DESC")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false)
    private Long sensorId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId; // Denormalized for partitioning

    @Column(nullable = false)
    private Double value;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "quality_score")
    private Integer qualityScore; // 0-100 data quality indicator

    @Column(name = "is_anomaly")
    @Builder.Default
    private Boolean isAnomaly = false;

    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData; // Original sensor payload if needed

    // Denormalized fields for faster queries (avoid JOINs on 800GB table)
    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "sensor_type", length = 50)
    private String sensorType;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}