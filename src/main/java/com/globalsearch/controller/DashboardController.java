package com.globalsearch.controller;

import com.globalsearch.document.DashboardDocument;
import com.globalsearch.entity.Dashboard;
import com.globalsearch.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Create a new dashboard
     * POST /api/dashboards
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Dashboard> createDashboard(@RequestBody Dashboard dashboard) {
        log.info("Creating new dashboard: {}", dashboard.getName());
        Dashboard createdDashboard = dashboardService.createDashboard(dashboard);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDashboard);
    }

    /**
     * Get dashboard by ID
     * GET /api/dashboards/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Dashboard> getDashboardById(@PathVariable Long id) {
        log.info("Fetching dashboard with ID: {}", id);
        Dashboard dashboard = dashboardService.getDashboardById(id);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get all dashboards for a tenant
     * GET /api/dashboards/tenant/{tenantId}
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<Dashboard>> getDashboardsByTenant(@PathVariable String tenantId) {
        log.info("Fetching dashboards for tenant: {}", tenantId);
        List<Dashboard> dashboards = dashboardService.getDashboardsByTenant(tenantId);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get paginated dashboards for a tenant
     * GET /api/dashboards/tenant/{tenantId}/page
     */
    @GetMapping("/tenant/{tenantId}/page")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<Dashboard>> getDashboardsByTenantPaginated(
            @PathVariable String tenantId,
            Pageable pageable) {
        log.info("Fetching paginated dashboards for tenant: {}", tenantId);
        Page<Dashboard> dashboards = dashboardService.getDashboardsByTenantPaginated(tenantId, pageable);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get dashboards owned by a user
     * GET /api/dashboards/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Dashboard>> getDashboardsByOwner(@PathVariable Long ownerId) {
        log.info("Fetching dashboards owned by user: {}", ownerId);
        List<Dashboard> dashboards = dashboardService.getDashboardsByOwner(ownerId);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get paginated dashboards for an owner
     * GET /api/dashboards/owner/{ownerId}/page
     */
    @GetMapping("/owner/{ownerId}/page")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Page<Dashboard>> getDashboardsByOwnerPaginated(
            @PathVariable Long ownerId,
            Pageable pageable) {
        log.info("Fetching paginated dashboards for owner: {}", ownerId);
        Page<Dashboard> dashboards = dashboardService.getDashboardsByOwnerPaginated(ownerId, pageable);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get shared dashboards for a tenant
     * GET /api/dashboards/tenant/{tenantId}/shared
     */
    @GetMapping("/tenant/{tenantId}/shared")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Dashboard>> getSharedDashboards(@PathVariable String tenantId) {
        log.info("Fetching shared dashboards for tenant: {}", tenantId);
        List<Dashboard> dashboards = dashboardService.getSharedDashboards(tenantId);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get favorite dashboards for a user
     * GET /api/dashboards/owner/{ownerId}/favorites
     */
    @GetMapping("/owner/{ownerId}/favorites")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Dashboard>> getFavoriteDashboards(@PathVariable Long ownerId) {
        log.info("Fetching favorite dashboards for user: {}", ownerId);
        List<Dashboard> dashboards = dashboardService.getFavoriteDashboards(ownerId);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get dashboards by type
     * GET /api/dashboards/tenant/{tenantId}/type/{type}
     */
    @GetMapping("/tenant/{tenantId}/type/{type}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Dashboard>> getDashboardsByType(
            @PathVariable String tenantId,
            @PathVariable Dashboard.DashboardType type) {
        log.info("Fetching dashboards for tenant {} with type: {}", tenantId, type);
        List<Dashboard> dashboards = dashboardService.getDashboardsByType(tenantId, type);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Get default dashboards for a tenant
     * GET /api/dashboards/tenant/{tenantId}/default
     */
    @GetMapping("/tenant/{tenantId}/default")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<Dashboard>> getDefaultDashboards(@PathVariable String tenantId) {
        log.info("Fetching default dashboards for tenant: {}", tenantId);
        List<Dashboard> dashboards = dashboardService.getDefaultDashboards(tenantId);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Update a dashboard
     * PUT /api/dashboards/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Dashboard> updateDashboard(
            @PathVariable Long id,
            @RequestBody Dashboard dashboardDetails) {
        log.info("Updating dashboard with ID: {}", id);
        Dashboard updatedDashboard = dashboardService.updateDashboard(id, dashboardDetails);
        return ResponseEntity.ok(updatedDashboard);
    }

    /**
     * Delete a dashboard
     * DELETE /api/dashboards/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<Void> deleteDashboard(@PathVariable Long id) {
        log.info("Deleting dashboard with ID: {}", id);
        dashboardService.deleteDashboard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search dashboards
     * GET /api/dashboards/search?tenantId={tenantId}&q={searchTerm}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<DashboardDocument>> searchDashboards(
            @RequestParam String tenantId,
            @RequestParam(required = false) String q) {
        log.info("Searching dashboards for tenant {} with term: {}", tenantId, q);
        List<DashboardDocument> dashboards = dashboardService.searchDashboards(tenantId, q);
        return ResponseEntity.ok(dashboards);
    }

    /**
     * Re-index all dashboards to Elasticsearch
     * POST /api/dashboards/reindex
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> reindexAllDashboards() {
        log.info("Re-indexing all dashboards to Elasticsearch");
        dashboardService.indexAllDashboards();
        return ResponseEntity.ok("Dashboards re-indexed successfully");
    }

    /**
     * Get total dashboard count
     * GET /api/dashboards/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Long> getTotalDashboardCount() {
        long count = dashboardService.getTotalDashboardCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get dashboard count by tenant
     * GET /api/dashboards/count/tenant/{tenantId}
     */
    @GetMapping("/count/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Long> getDashboardCountByTenant(@PathVariable String tenantId) {
        Long count = dashboardService.getDashboardCountByTenant(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get dashboard count by owner
     * GET /api/dashboards/count/owner/{ownerId}
     */
    @GetMapping("/count/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Long> getDashboardCountByOwner(@PathVariable Long ownerId) {
        Long count = dashboardService.getDashboardCountByOwner(ownerId);
        return ResponseEntity.ok(count);
    }
}
