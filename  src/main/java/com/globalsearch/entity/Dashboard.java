package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboards",
        indexes = {
                @Index(name = "idx_dashboard_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dashboard_owner", columnList = "owner_id"),
                @Index(name = "idx_dashboard_shared", columnList = "is_shared")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_name", length = 100)
    private String ownerName; // Denormalized for display

    @Column(length = 1000)
    private String description;

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration; // JSON configuration for widgets

    @Column(name = "layout", columnDefinition = "TEXT")
    private String layout; // JSON layout structure

    @Enumerated(EnumType.STRING)
    @Column(name = "dashboard_type", length = 50)
    @Builder.Default
    private DashboardType dashboardType = DashboardType.CUSTOM;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_shared")
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "is_favorite")
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "refresh_interval")
    private Integer refreshInterval; // in seconds

    @Column(name = "tags", length = 500)
    private String tags; // comma-separated

    @Column(name = "access_count")
    @Builder.Default
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Widget configuration
    @Column(name = "widgets", columnDefinition = "TEXT")
    private String widgets; // JSON array of widget configurations

    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters; // JSON - default filters for this dashboard

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions; // JSON - who can view/edit

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (accessCount == null) {
            accessCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DashboardType {
        OVERVIEW,       // General overview dashboard
        SENSORS,        // Sensor monitoring dashboard
        ALERTS,         // Alert management dashboard
        PERFORMANCE,    // Performance metrics dashboard
        ANALYTICS,      // Data analytics dashboard
        CUSTOM         // User-created custom dashboard
    }

    // Helper method to increment access
    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        this.lastAccessedAt = LocalDateTime.now();
    }
}