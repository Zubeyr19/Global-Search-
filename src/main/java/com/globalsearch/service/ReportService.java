package com.globalsearch.service;

import com.globalsearch.document.ReportDocument;
import com.globalsearch.entity.Report;
import com.globalsearch.repository.ReportRepository;
import com.globalsearch.repository.search.ReportSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportSearchRepository reportSearchRepository;

    /**
     * Create a new report
     */
    @Transactional
    public Report createReport(Report report) {
        log.info("Creating new report: {}", report.getName());

        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        if (report.getUpdatedAt() == null) {
            report.setUpdatedAt(LocalDateTime.now());
        }

        // Save to MySQL
        Report savedReport = reportRepository.save(report);

        // Index in Elasticsearch
        ReportDocument document = ReportDocument.fromEntity(savedReport);
        reportSearchRepository.save(document);

        log.info("Report created successfully with ID: {}", savedReport.getId());
        return savedReport;
    }

    /**
     * Get report by ID
     */
    public Report getReportById(Long id) {
        log.info("Fetching report with ID: {}", id);
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found: " + id));
    }

    /**
     * Get all reports for a tenant
     */
    public List<Report> getReportsByTenant(String tenantId) {
        log.info("Fetching reports for tenant: {}", tenantId);
        return reportRepository.findByTenantId(tenantId);
    }

    /**
     * Get reports by tenant with pagination
     */
    public Page<Report> getReportsByTenantPaginated(String tenantId, Pageable pageable) {
        log.info("Fetching paginated reports for tenant: {}", tenantId);
        return reportRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get reports by user
     */
    public List<Report> getReportsByUser(Long userId) {
        log.info("Fetching reports created by user: {}", userId);
        return reportRepository.findByCreatedBy(userId);
    }

    /**
     * Get reports by status
     */
    public List<Report> getReportsByStatus(Report.ReportStatus status) {
        log.info("Fetching reports with status: {}", status);
        return reportRepository.findByStatus(status);
    }

    /**
     * Get reports by tenant and type
     */
    public List<Report> getReportsByTenantAndType(String tenantId, String reportType) {
        log.info("Fetching reports for tenant {} with type: {}", tenantId, reportType);
        return reportRepository.findByTenantIdAndReportType(tenantId, reportType);
    }

    /**
     * Update report
     */
    @Transactional
    public Report updateReport(Long id, Report reportDetails) {
        log.info("Updating report with ID: {}", id);

        Report report = getReportById(id);

        // Update fields
        if (reportDetails.getName() != null) {
            report.setName(reportDetails.getName());
        }
        if (reportDetails.getDescription() != null) {
            report.setDescription(reportDetails.getDescription());
        }
        if (reportDetails.getReportType() != null) {
            report.setReportType(reportDetails.getReportType());
        }
        if (reportDetails.getStatus() != null) {
            report.setStatus(reportDetails.getStatus());
        }
        if (reportDetails.getTags() != null) {
            report.setTags(reportDetails.getTags());
        }
        if (reportDetails.getIsPublic() != null) {
            report.setIsPublic(reportDetails.getIsPublic());
        }
        if (reportDetails.getExpiresAt() != null) {
            report.setExpiresAt(reportDetails.getExpiresAt());
        }

        report.setUpdatedAt(LocalDateTime.now());

        // Save to MySQL
        Report updatedReport = reportRepository.save(report);

        // Update Elasticsearch
        ReportDocument document = ReportDocument.fromEntity(updatedReport);
        reportSearchRepository.save(document);

        log.info("Report updated successfully");
        return updatedReport;
    }

    /**
     * Delete report
     */
    @Transactional
    public void deleteReport(Long id) {
        log.info("Deleting report with ID: {}", id);

        // Delete from MySQL
        reportRepository.deleteById(id);

        // Delete from Elasticsearch
        reportSearchRepository.deleteById(id);

        log.info("Report deleted successfully");
    }

    /**
     * Search reports in Elasticsearch
     */
    public List<ReportDocument> searchReports(String tenantId, String searchTerm) {
        log.info("Searching reports for tenant {} with term: {}", tenantId, searchTerm);

        if (searchTerm == null || searchTerm.isEmpty()) {
            return reportSearchRepository.findByTenantId(tenantId);
        }

        return reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, searchTerm);
    }

    /**
     * Index all reports to Elasticsearch
     */
    @Transactional
    public void indexAllReports() {
        log.info("Indexing all reports to Elasticsearch");

        List<Report> allReports = reportRepository.findAll();
        List<ReportDocument> documents = allReports.stream()
                .map(ReportDocument::fromEntity)
                .collect(Collectors.toList());

        reportSearchRepository.saveAll(documents);

        log.info("Indexed {} reports to Elasticsearch", documents.size());
    }

    /**
     * Get total report count
     */
    public long getTotalReportCount() {
        return reportRepository.count();
    }

    /**
     * Get report count by tenant
     */
    public Long getReportCountByTenant(String tenantId) {
        return reportRepository.countByTenantId(tenantId);
    }
}
