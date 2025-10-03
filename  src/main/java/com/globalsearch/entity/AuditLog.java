package com.globalsearch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user_id", columnList = "user_id"),
                @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
                @Index(name = "idx_audit_tenant", columnList = "tenant_id"),
                @Index(name = "idx_audit_action", columnList = "action"),
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", length = 50)
    private String username; // Denormalized

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName; // Denormalized for display

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // JSON format

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // JSON format

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_method", length = 10)
    private String requestMethod; // GET, POST, PUT, DELETE

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "response_status")
    private Integer responseStatus; // HTTP status code

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum AuditAction {
        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        TOKEN_REFRESH,
        PASSWORD_CHANGE,

        // CRUD Operations
        CREATE,
        READ,
        UPDATE,
        DELETE,
        BULK_UPDATE,
        BULK_DELETE,

        // Search Operations
        SEARCH,
        EXPORT,

        // Admin Operations
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        PERMISSION_CHANGE,
        ROLE_ASSIGNMENT,

        // System Operations
        CONFIG_CHANGE,
        SYSTEM_RESTART,
        BACKUP_CREATED,
        DATA_MIGRATION,

        // Security Events
        ACCESS_DENIED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED
    }
}