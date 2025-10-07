package com.globalsearch.repository;

import com.globalsearch.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by user
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    // Find by tenant
    Page<AuditLog> findByTenantId(String tenantId, Pageable pageable);

    // Find by action
    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    // Find by entity
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    // Find by timestamp range
    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Find by user and date range
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find by tenant and date range
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByTenantIdAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find by action and date range
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action AND a.timestamp BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByActionAndDateRange(
            @Param("action") AuditLog.AuditAction action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Complex search with multiple filters
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:tenantId IS NULL OR a.tenantId = :tenantId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> searchAuditLogs(
            @Param("userId") Long userId,
            @Param("tenantId") String tenantId,
            @Param("action") AuditLog.AuditAction action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Find failed login attempts
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.timestamp >= :since")
    List<AuditLog> findFailedLoginAttemptsSince(@Param("since") LocalDateTime since);

    // Find suspicious activities
    @Query("SELECT a FROM AuditLog a WHERE a.action IN ('ACCESS_DENIED', 'SUSPICIOUS_ACTIVITY', 'RATE_LIMIT_EXCEEDED') " +
            "AND a.timestamp >= :since")
    List<AuditLog> findSuspiciousActivitiesSince(@Param("since") LocalDateTime since);

    // Count actions by user
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.action = :action")
    Long countActionsByUser(@Param("userId") Long userId, @Param("action") AuditLog.AuditAction action);

    // Count actions by tenant
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.timestamp BETWEEN :startDate AND :endDate")
    Long countActionsByTenantInDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Get recent activity for user
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentActivityByUser(@Param("userId") Long userId, Pageable pageable);

    // Get recent activity for tenant
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentActivityByTenant(@Param("tenantId") String tenantId, Pageable pageable);

    // Delete old audit logs (for cleanup)
    void deleteByTimestampBefore(LocalDateTime date);
}
