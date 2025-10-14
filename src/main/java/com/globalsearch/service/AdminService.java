package com.globalsearch.service;

import com.globalsearch.dto.response.RecentActivityDTO;
import com.globalsearch.dto.response.SystemOverviewResponse;
import com.globalsearch.dto.response.TenantInfoResponse;
import com.globalsearch.entity.AuditLog;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.User;
import com.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReportRepository reportRepository;
    private final DashboardRepository dashboardRepository;

    /**
     * Get system overview with comprehensive statistics
     */
    public SystemOverviewResponse getSystemOverview() {
        log.info("Fetching system overview statistics");

        Long totalUsers = userRepository.count();
        Long totalCompanies = companyRepository.count();
        Long totalLocations = locationRepository.count();
        Long totalZones = zoneRepository.count();
        Long totalSensors = sensorRepository.count();
        Long totalReports = reportRepository.count();
        Long totalDashboards = dashboardRepository.count();

        // Count unique tenants
        Long totalTenants = userRepository.countDistinctTenantIds();

        // Count total searches from audit logs
        Long totalSearches = auditLogRepository.countByAction(AuditLog.AuditAction.SEARCH);

        // Active users (logged in within last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        Long activeUsers = auditLogRepository.countDistinctUsersSince(yesterday);

        // Users by role
        Map<String, Long> usersByRole = userRepository.findAll().stream()
                .flatMap(user -> user.getRoles().stream())
                .collect(Collectors.groupingBy(Role -> Role.name(), Collectors.counting()));

        // Companies by status
        Map<String, Long> companiesByStatus = companyRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        company -> company.getStatus() != null ? company.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));

        // Recent activity (last 10 actions)
        List<RecentActivityDTO> recentActivity = getRecentActivity(10);

        // Average search time
        Double averageSearchTime = auditLogRepository.getAverageResponseTimeByAction(AuditLog.AuditAction.SEARCH);
        if (averageSearchTime == null) {
            averageSearchTime = 0.0;
        }

        // Total audit logs
        Long totalAuditLogs = auditLogRepository.count();

        // Searches by day (last 7 days)
        Map<String, Long> searchesByDay = getSearchesByDay(7);

        return SystemOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalCompanies(totalCompanies)
                .totalLocations(totalLocations)
                .totalZones(totalZones)
                .totalSensors(totalSensors)
                .totalSearches(totalSearches)
                .totalTenants(totalTenants)
                .activeUsers(activeUsers)
                .totalReports(totalReports)
                .totalDashboards(totalDashboards)
                .usersByRole(usersByRole)
                .companiesByStatus(companiesByStatus)
                .recentActivity(recentActivity)
                .averageSearchTime(averageSearchTime)
                .totalAuditLogs(totalAuditLogs)
                .searchesByDay(searchesByDay)
                .build();
    }

    /**
     * Get all tenants with their statistics
     */
    public List<TenantInfoResponse> getAllTenants() {
        log.info("Fetching all tenant information");

        List<String> tenantIds = userRepository.findDistinctTenantIds();

        return tenantIds.stream()
                .map(this::getTenantInfo)
                .collect(Collectors.toList());
    }

    /**
     * Get information for a specific tenant
     */
    public TenantInfoResponse getTenantInfo(String tenantId) {
        log.info("Fetching info for tenant: {}", tenantId);

        Long userCount = userRepository.countByTenantId(tenantId);
        Long companyCount = companyRepository.countByTenantId(tenantId);
        Long locationCount = locationRepository.countByTenantId(tenantId);
        Long sensorCount = sensorRepository.countByTenantId(tenantId);
        Long searchCount = auditLogRepository.countByTenantIdAndAction(tenantId, AuditLog.AuditAction.SEARCH);

        // Find the oldest user for this tenant to get creation date
        LocalDateTime createdAt = userRepository.findFirstByTenantIdOrderByCreatedAtAsc(tenantId)
                .map(User::getCreatedAt)
                .orElse(null);

        // Get last activity
        LocalDateTime lastActivity = auditLogRepository.findFirstByTenantIdOrderByTimestampDesc(tenantId)
                .map(AuditLog::getTimestamp)
                .orElse(null);

        // Get tenant name from first company
        String tenantName = companyRepository.findFirstByTenantId(tenantId)
                .map(Company::getName)
                .orElse("Tenant " + tenantId);

        // Calculate storage (simplified - would need actual calculation in production)
        Long storageUsedMB = (userCount + companyCount + sensorCount) * 10; // Simplified calculation

        return TenantInfoResponse.builder()
                .tenantId(tenantId)
                .tenantName(tenantName)
                .userCount(userCount)
                .companyCount(companyCount)
                .locationCount(locationCount)
                .sensorCount(sensorCount)
                .searchCount(searchCount)
                .status("ACTIVE") // TODO: Add actual tenant status
                .createdAt(createdAt)
                .lastActivity(lastActivity)
                .storageUsedMB(storageUsedMB)
                .build();
    }

    /**
     * Get recent activity across the system
     */
    public List<RecentActivityDTO> getRecentActivity(int limit) {
        log.info("Fetching recent activity (limit: {})", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());

        return auditLogRepository.findAll(pageable).stream()
                .map(log -> RecentActivityDTO.builder()
                        .userId(log.getUserId())
                        .username(log.getUsername())
                        .tenantId(log.getTenantId())
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .timestamp(log.getTimestamp())
                        .ipAddress(log.getIpAddress())
                        .requestMethod(log.getRequestMethod())
                        .responseStatus(log.getResponseStatus())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get all users in the system
     */
    public List<User> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Get users by tenant
     */
    public List<User> getUsersByTenant(String tenantId) {
        log.info("Fetching users for tenant: {}", tenantId);
        return userRepository.findByTenantId(tenantId);
    }

    /**
     * Update user roles
     */
    @Transactional
    public User updateUserRoles(Long userId, Set<User.Role> roles) {
        log.info("Updating roles for user {}: {}", userId, roles);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * Get searches by day for the last N days
     */
    private Map<String, Long> getSearchesByDay(int days) {
        Map<String, Long> searchesByDay = new HashMap<>();

        for (int i = 0; i < days; i++) {
            LocalDateTime startOfDay = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            long count = auditLogRepository.findByActionAndDateRange(
                    AuditLog.AuditAction.SEARCH,
                    startOfDay,
                    endOfDay,
                    Pageable.unpaged()
            ).getTotalElements();

            String dayKey = startOfDay.toLocalDate().toString();
            searchesByDay.put(dayKey, count);
        }

        return searchesByDay;
    }

    /**
     * Get activity for specific tenant
     */
    public List<RecentActivityDTO> getTenantActivity(String tenantId, int limit) {
        log.info("Fetching activity for tenant {} (limit: {})", tenantId, limit);

        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findRecentActivityByTenant(tenantId, pageable).stream()
                .map(log -> RecentActivityDTO.builder()
                        .userId(log.getUserId())
                        .username(log.getUsername())
                        .tenantId(log.getTenantId())
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .timestamp(log.getTimestamp())
                        .ipAddress(log.getIpAddress())
                        .requestMethod(log.getRequestMethod())
                        .responseStatus(log.getResponseStatus())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get activity for specific user
     */
    public List<RecentActivityDTO> getUserActivity(Long userId, int limit) {
        log.info("Fetching activity for user {} (limit: {})", userId, limit);

        Pageable pageable = PageRequest.of(0, limit);
        return auditLogRepository.findRecentActivityByUser(userId, pageable).stream()
                .map(log -> RecentActivityDTO.builder()
                        .userId(log.getUserId())
                        .username(log.getUsername())
                        .tenantId(log.getTenantId())
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .entityName(log.getEntityName())
                        .timestamp(log.getTimestamp())
                        .ipAddress(log.getIpAddress())
                        .requestMethod(log.getRequestMethod())
                        .responseStatus(log.getResponseStatus())
                        .build())
                .collect(Collectors.toList());
    }
}
