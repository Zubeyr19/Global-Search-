package com.globalsearch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsearch.entity.AuditLog;
import com.globalsearch.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an audit event asynchronously
     */
    @Async
    public void logEvent(AuditLog.AuditAction action, Long userId, String username, String tenantId,
                         String entityType, Long entityId, String entityName,
                         Object oldValue, Object newValue, HttpServletRequest request) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .username(username)
                    .tenantId(tenantId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .timestamp(LocalDateTime.now())
                    .build();

            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURI());
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} by user {} on {} {}", action, username, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Log authentication event
     */
    @Async
    public void logAuthEvent(AuditLog.AuditAction action, Long userId, String username, String tenantId,
                             HttpServletRequest request, Integer responseStatus, String errorMessage) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .username(username)
                    .tenantId(tenantId != null ? tenantId : "SYSTEM")
                    .entityType("AUTH")
                    .timestamp(LocalDateTime.now())
                    .responseStatus(responseStatus)
                    .errorMessage(errorMessage)
                    .build();

            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURI());
            }

            auditLogRepository.save(auditLog);
            log.debug("Auth audit log created: {} for user {}", action, username);
        } catch (Exception e) {
            log.error("Failed to create auth audit log", e);
        }
    }

    /**
     * Log search event
     */
    @Async
    public void logSearchEvent(Long userId, String username, String tenantId,
                               String searchQuery, int resultCount, HttpServletRequest request) {
        try {
            Map<String, Object> searchDetails = Map.of(
                    "query", searchQuery != null ? searchQuery : "",
                    "resultCount", resultCount
            );

            AuditLog auditLog = AuditLog.builder()
                    .action(AuditLog.AuditAction.SEARCH)
                    .userId(userId)
                    .username(username)
                    .tenantId(tenantId)
                    .entityType("SEARCH")
                    .newValue(toJson(searchDetails))
                    .timestamp(LocalDateTime.now())
                    .build();

            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestMethod(request.getMethod());
                auditLog.setRequestUrl(request.getRequestURI());
            }

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to create search audit log", e);
        }
    }

    /**
     * Get all audit logs with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs by user ID
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get audit logs by username
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    /**
     * Get audit logs by tenant ID
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByTenantId(String tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get audit logs by action
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    /**
     * Get audit logs by date range
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate, pageable);
    }

    /**
     * Search audit logs with multiple filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> searchAuditLogs(Long userId, String tenantId, AuditLog.AuditAction action,
                                          String entityType, LocalDateTime startDate, LocalDateTime endDate,
                                          Pageable pageable) {
        return auditLogRepository.searchAuditLogs(userId, tenantId, action, entityType, startDate, endDate, pageable);
    }

    /**
     * Get failed login attempts since a specific time
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getFailedLoginAttempts(LocalDateTime since) {
        return auditLogRepository.findFailedLoginAttemptsSince(since);
    }

    /**
     * Get suspicious activities since a specific time
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getSuspiciousActivities(LocalDateTime since) {
        return auditLogRepository.findSuspiciousActivitiesSince(since);
    }

    /**
     * Get audit statistics for a tenant
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStatistics(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        Long totalActions = auditLogRepository.countActionsByTenantInDateRange(tenantId, startDate, endDate);
        List<AuditLog> failedLogins = auditLogRepository.findFailedLoginAttemptsSince(startDate);
        List<AuditLog> suspiciousActivities = auditLogRepository.findSuspiciousActivitiesSince(startDate);

        return Map.of(
                "totalActions", totalActions,
                "failedLoginAttempts", failedLogins.size(),
                "suspiciousActivities", suspiciousActivities.size(),
                "dateRange", Map.of("start", startDate, "end", endDate)
        );
    }

    /**
     * Delete old audit logs (for data retention policy)
     */
    public void deleteAuditLogsOlderThan(LocalDateTime date) {
        auditLogRepository.deleteByTimestampBefore(date);
        log.info("Deleted audit logs older than {}", date);
    }

    /**
     * Convert object to JSON string
     */
    private String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON", e);
            return object.toString();
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
