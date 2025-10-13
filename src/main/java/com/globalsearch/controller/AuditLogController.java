package com.globalsearch.controller;

import com.globalsearch.dto.request.AuditLogSearchRequest;
import com.globalsearch.dto.response.AuditLogResponse;
import com.globalsearch.entity.AuditLog;
import com.globalsearch.security.JwtTokenProvider;
import com.globalsearch.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * GET /api/audit-logs - Get all audit logs (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AuditLog> auditLogs = auditLogService.getAllAuditLogs(pageable);
        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/user/{userId} - Get audit logs for a specific user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByUserId(userId, pageable);
        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/username/{username} - Get audit logs by username
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByUsername(username, pageable);
        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/tenant/{tenantId} - Get audit logs by tenant
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByTenant(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByTenantId(tenantId, pageable);
        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/action/{action} - Get audit logs by action
     */
    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable AuditLog.AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByAction(action, pageable);
        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/audit-logs/search - Search audit logs with filters
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestBody AuditLogSearchRequest searchRequest) {

        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;
        String sortBy = searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "timestamp";
        String sortDirection = searchRequest.getSortDirection() != null ? searchRequest.getSortDirection() : "DESC";

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AuditLog> auditLogs = auditLogService.searchAuditLogs(
                searchRequest.getUserId(),
                searchRequest.getTenantId(),
                searchRequest.getAction(),
                searchRequest.getEntityType(),
                searchRequest.getStartDate(),
                searchRequest.getEndDate(),
                pageable
        );

        Page<AuditLogResponse> response = auditLogs.map(AuditLogResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/failed-logins - Get failed login attempts
     */
    @GetMapping("/failed-logins")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getFailedLoginAttempts(
            @RequestParam(required = false) Integer hours) {

        int hoursToCheck = hours != null ? hours : 24;
        LocalDateTime since = LocalDateTime.now().minusHours(hoursToCheck);

        List<AuditLog> failedLogins = auditLogService.getFailedLoginAttempts(since);
        List<AuditLogResponse> response = failedLogins.stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/suspicious - Get suspicious activities
     */
    @GetMapping("/suspicious")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getSuspiciousActivities(
            @RequestParam(required = false) Integer hours) {

        int hoursToCheck = hours != null ? hours : 24;
        LocalDateTime since = LocalDateTime.now().minusHours(hoursToCheck);

        List<AuditLog> suspiciousActivities = auditLogService.getSuspiciousActivities(since);
        List<AuditLogResponse> response = suspiciousActivities.stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/audit-logs/stats - Get audit statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
            @RequestParam String tenantId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

        Map<String, Object> statistics = auditLogService.getAuditStatistics(tenantId, start, end);
        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /api/audit-logs/actions - Get available audit actions
     */
    @GetMapping("/actions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<String>> getAvailableActions() {
        List<String> actions = List.of(AuditLog.AuditAction.values())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        return ResponseEntity.ok(actions);
    }

    /**
     * DELETE /api/audit-logs/cleanup - Delete old audit logs
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> cleanupOldAuditLogs(
            @RequestParam(required = false) Integer daysToKeep) {

        int days = daysToKeep != null ? daysToKeep : 90; // Default: keep 90 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        auditLogService.deleteAuditLogsOlderThan(cutoffDate);

        return ResponseEntity.ok(Map.of(
                "message", "Old audit logs deleted successfully",
                "cutoffDate", cutoffDate.toString(),
                "daysKept", String.valueOf(days)
        ));
    }
}
