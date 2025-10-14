package com.globalsearch.controller.admin;

import com.globalsearch.dto.request.UserRoleUpdateRequest;
import com.globalsearch.dto.response.RecentActivityDTO;
import com.globalsearch.dto.response.SystemOverviewResponse;
import com.globalsearch.dto.response.TenantInfoResponse;
import com.globalsearch.entity.User;
import com.globalsearch.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    /**
     * Get comprehensive system overview with statistics
     * GET /api/admin/overview
     *
     * Returns: Total counts, active users, role distribution, company status breakdown,
     * recent activity, performance metrics, and searches by day
     *
     * Access: SUPER_ADMIN, TENANT_ADMIN
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<SystemOverviewResponse> getSystemOverview() {
        log.info("Admin requesting system overview");
        SystemOverviewResponse overview = adminService.getSystemOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * Get all tenants with statistics
     * GET /api/admin/tenants
     *
     * Returns: List of all tenants with user counts, company counts, search activity,
     * creation dates, and last activity timestamps
     *
     * Access: SUPER_ADMIN only
     */
    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<TenantInfoResponse>> getAllTenants() {
        log.info("Admin requesting all tenants");
        List<TenantInfoResponse> tenants = adminService.getAllTenants();
        return ResponseEntity.ok(tenants);
    }

    /**
     * Get specific tenant information
     * GET /api/admin/tenants/{tenantId}
     *
     * @param tenantId - The tenant identifier
     * Returns: Detailed tenant information including all statistics
     *
     * Access: SUPER_ADMIN only
     */
    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantInfoResponse> getTenantInfo(@PathVariable String tenantId) {
        log.info("Admin requesting info for tenant {}", tenantId);
        TenantInfoResponse tenant = adminService.getTenantInfo(tenantId);
        return ResponseEntity.ok(tenant);
    }

    /**
     * Get recent activity across all tenants
     * GET /api/admin/activity?limit=50
     *
     * @param limit - Number of recent activities to return (default: 50, max: 1000)
     * Returns: List of recent user activities with timestamps, actions, and metadata
     *
     * Access: SUPER_ADMIN, TENANT_ADMIN
     */
    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<RecentActivityDTO>> getRecentActivity(
            @RequestParam(defaultValue = "50") int limit) {

        // Limit maximum to prevent performance issues
        if (limit > 1000) {
            limit = 1000;
        }

        log.info("Admin requesting recent activity (limit: {})", limit);
        List<RecentActivityDTO> activity = adminService.getRecentActivity(limit);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get activity for specific tenant
     * GET /api/admin/tenants/{tenantId}/activity?limit=50
     *
     * @param tenantId - The tenant identifier
     * @param limit - Number of recent activities to return
     * Returns: Recent activities for the specified tenant
     *
     * Access: SUPER_ADMIN only
     */
    @GetMapping("/tenants/{tenantId}/activity")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<RecentActivityDTO>> getTenantActivity(
            @PathVariable String tenantId,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Admin requesting activity for tenant {} (limit: {})", tenantId, limit);
        List<RecentActivityDTO> activity = adminService.getTenantActivity(tenantId, limit);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get activity for specific user
     * GET /api/admin/users/{userId}/activity?limit=50
     *
     * @param userId - The user identifier
     * @param limit - Number of recent activities to return
     * Returns: Recent activities for the specified user
     *
     * Access: SUPER_ADMIN, TENANT_ADMIN
     */
    @GetMapping("/users/{userId}/activity")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<RecentActivityDTO>> getUserActivity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Admin requesting activity for user {} (limit: {})", userId, limit);
        List<RecentActivityDTO> activity = adminService.getUserActivity(userId, limit);
        return ResponseEntity.ok(activity);
    }

    /**
     * Get all users in the system
     * GET /api/admin/users
     *
     * Returns: List of all users (passwords excluded)
     *
     * Access: SUPER_ADMIN, TENANT_ADMIN
     */
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Admin requesting all users");
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get users by tenant
     * GET /api/admin/tenants/{tenantId}/users
     *
     * @param tenantId - The tenant identifier
     * Returns: List of users for the specified tenant
     *
     * Access: SUPER_ADMIN only
     */
    @GetMapping("/tenants/{tenantId}/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<User>> getUsersByTenant(@PathVariable String tenantId) {
        log.info("Admin requesting users for tenant {}", tenantId);
        List<User> users = adminService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(users);
    }

    /**
     * Update user roles
     * PUT /api/admin/users/{userId}/roles
     *
     * @param userId - The user identifier
     * @param request - Request body containing the new set of roles
     * Returns: Updated user object
     *
     * Request body example:
     * {
     *   "roles": ["MANAGER", "OPERATOR"]
     * }
     *
     * Access: SUPER_ADMIN, TENANT_ADMIN
     */
    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<User> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody UserRoleUpdateRequest request) {

        log.info("Admin updating roles for user {}: {}", userId, request.getRoles());
        User updatedUser = adminService.updateUserRoles(userId, request.getRoles());
        return ResponseEntity.ok(updatedUser);
    }
}
