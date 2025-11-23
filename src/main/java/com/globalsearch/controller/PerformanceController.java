package com.globalsearch.controller;

import com.globalsearch.service.PerformanceMetricsService;
import com.globalsearch.service.PerformanceMetricsService.PerformanceStats;
import com.globalsearch.service.PerformanceMetricsService.QueryMetric;
import com.globalsearch.service.PerformanceMetricsService.SLAComplianceReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for performance metrics and monitoring
 * Requirements: F3, F7.1-7.4, U16 - Performance monitoring
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Monitoring", description = "Endpoints for system performance metrics")
public class PerformanceController {

    private final PerformanceMetricsService metricsService;

    /**
     * Get overall performance statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get overall performance statistics",
        description = "Returns aggregate performance metrics for all queries")
    public ResponseEntity<PerformanceStats> getOverallStats() {
        return ResponseEntity.ok(metricsService.getOverallStats());
    }

    /**
     * Get performance statistics for specific tenant
     */
    @GetMapping("/stats/tenant/{tenantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get tenant-specific performance statistics",
        description = "Returns performance metrics for a specific tenant")
    public ResponseEntity<PerformanceStats> getTenantStats(@PathVariable String tenantId) {
        return ResponseEntity.ok(metricsService.getTenantStats(tenantId));
    }

    /**
     * Get recent slow queries
     */
    @GetMapping("/slow-queries")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get recent slow queries",
        description = "Returns queries that exceeded 1 second execution time")
    public ResponseEntity<List<QueryMetric>> getSlowQueries(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(metricsService.getSlowQueries(limit));
    }

    /**
     * Get latency distribution
     */
    @GetMapping("/distribution")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get latency distribution",
        description = "Returns distribution of queries across latency buckets")
    public ResponseEntity<Map<String, Integer>> getLatencyDistribution() {
        return ResponseEntity.ok(metricsService.getLatencyDistribution());
    }

    /**
     * Check SLA compliance
     */
    @GetMapping("/sla-compliance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Check SLA compliance",
        description = "Validates if performance meets defined SLA requirements (F3: <1s latency, F3.1: <500ms avg)")
    public ResponseEntity<SLAComplianceReport> checkSLACompliance() {
        return ResponseEntity.ok(metricsService.checkSLACompliance());
    }

    /**
     * Clear metrics (admin only, for testing)
     */
    @DeleteMapping("/metrics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Clear performance metrics",
        description = "Clears all stored performance metrics (admin only)")
    public ResponseEntity<Map<String, String>> clearMetrics() {
        metricsService.clearMetrics();
        return ResponseEntity.ok(Map.of("message", "Performance metrics cleared successfully"));
    }
}
