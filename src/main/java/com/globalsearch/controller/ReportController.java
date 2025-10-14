package com.globalsearch.controller;

import com.globalsearch.document.ReportDocument;
import com.globalsearch.entity.Report;
import com.globalsearch.service.ReportService;
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * Create a new report
     * POST /api/reports
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        log.info("Creating new report: {}", report.getName());
        Report createdReport = reportService.createReport(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReport);
    }

    /**
     * Get report by ID
     * GET /api/reports/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<Report> getReportById(@PathVariable Long id) {
        log.info("Fetching report with ID: {}", id);
        Report report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports for a tenant
     * GET /api/reports/tenant/{tenantId}
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<Report>> getReportsByTenant(@PathVariable String tenantId) {
        log.info("Fetching reports for tenant: {}", tenantId);
        List<Report> reports = reportService.getReportsByTenant(tenantId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get paginated reports for a tenant
     * GET /api/reports/tenant/{tenantId}/page
     */
    @GetMapping("/tenant/{tenantId}/page")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<Report>> getReportsByTenantPaginated(
            @PathVariable String tenantId,
            Pageable pageable) {
        log.info("Fetching paginated reports for tenant: {}", tenantId);
        Page<Report> reports = reportService.getReportsByTenantPaginated(tenantId, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get reports created by a user
     * GET /api/reports/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable Long userId) {
        log.info("Fetching reports created by user: {}", userId);
        List<Report> reports = reportService.getReportsByUser(userId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get reports by status
     * GET /api/reports/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<Report>> getReportsByStatus(@PathVariable Report.ReportStatus status) {
        log.info("Fetching reports with status: {}", status);
        List<Report> reports = reportService.getReportsByStatus(status);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get reports by tenant and type
     * GET /api/reports/tenant/{tenantId}/type/{reportType}
     */
    @GetMapping("/tenant/{tenantId}/type/{reportType}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<List<Report>> getReportsByTenantAndType(
            @PathVariable String tenantId,
            @PathVariable String reportType) {
        log.info("Fetching reports for tenant {} with type: {}", tenantId, reportType);
        List<Report> reports = reportService.getReportsByTenantAndType(tenantId, reportType);
        return ResponseEntity.ok(reports);
    }

    /**
     * Update a report
     * PUT /api/reports/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Report> updateReport(
            @PathVariable Long id,
            @RequestBody Report reportDetails) {
        log.info("Updating report with ID: {}", id);
        Report updatedReport = reportService.updateReport(id, reportDetails);
        return ResponseEntity.ok(updatedReport);
    }

    /**
     * Delete a report
     * DELETE /api/reports/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        log.info("Deleting report with ID: {}", id);
        reportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Search reports
     * GET /api/reports/search?tenantId={tenantId}&q={searchTerm}
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<ReportDocument>> searchReports(
            @RequestParam String tenantId,
            @RequestParam(required = false) String q) {
        log.info("Searching reports for tenant {} with term: {}", tenantId, q);
        List<ReportDocument> reports = reportService.searchReports(tenantId, q);
        return ResponseEntity.ok(reports);
    }

    /**
     * Re-index all reports to Elasticsearch
     * POST /api/reports/reindex
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> reindexAllReports() {
        log.info("Re-indexing all reports to Elasticsearch");
        reportService.indexAllReports();
        return ResponseEntity.ok("Reports re-indexed successfully");
    }

    /**
     * Get total report count
     * GET /api/reports/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Long> getTotalReportCount() {
        long count = reportService.getTotalReportCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get report count by tenant
     * GET /api/reports/count/tenant/{tenantId}
     */
    @GetMapping("/count/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<Long> getReportCountByTenant(@PathVariable String tenantId) {
        Long count = reportService.getReportCountByTenant(tenantId);
        return ResponseEntity.ok(count);
    }
}
