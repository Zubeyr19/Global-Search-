package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "tenant_id"),
                @UniqueConstraint(columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_company_tenant", columnList = "tenant_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"locations", "users"})
@EqualsAndHashCode(exclude = {"locations", "users"})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "tenant_id", nullable = false, unique = true, length = 50)
    private String tenantId;

    @Column(length = 50)
    private String industry;

    @Column(length = 500)
    private String description;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "max_users")
    @Builder.Default
    private Integer maxUsers = 10;

    @Column(name = "max_locations")
    @Builder.Default
    private Integer maxLocations = 5;

    @Column(name = "max_sensors")
    @Builder.Default
    private Integer maxSensors = 100;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Location> locations = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tenantId == null) {
            tenantId = "TENANT_" + name.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CompanyStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        TRIAL
    }

    // Helper methods
    public boolean isActive() {
        return status == CompanyStatus.ACTIVE;
    }

    public boolean canAddUsers(int count) {
        return users.size() + count <= maxUsers;
    }

    public boolean canAddLocations(int count) {
        return locations.size() + count <= maxLocations;
    }
}