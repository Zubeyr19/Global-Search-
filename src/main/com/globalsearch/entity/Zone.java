package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "zones",
        indexes = {
                @Index(name = "idx_zone_location", columnList = "location_id"),
                @Index(name = "idx_zone_name", columnList = "name")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"location", "sensors"})
@EqualsAndHashCode(exclude = {"location", "sensors"})
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String type; // storage, production, office, parking

    @Column(length = 500)
    private String description;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "area_size")
    private Double areaSize; // in square meters

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Sensor> sensors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ZoneStatus status = ZoneStatus.ACTIVE;

    @Column(name = "temperature_min")
    private Double temperatureMin;

    @Column(name = "temperature_max")
    private Double temperatureMax;

    @Column(name = "humidity_min")
    private Double humidityMin;

    @Column(name = "humidity_max")
    private Double humidityMax;

    @Column(name = "alert_enabled")
    @Builder.Default
    private Boolean alertEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ZoneStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        RESTRICTED
    }

    public boolean isActive() {
        return status == ZoneStatus.ACTIVE;
    }
}