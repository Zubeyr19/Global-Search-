package com.globalsearch.repository.search;

import com.globalsearch.document.ReportDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSearchRepository extends ElasticsearchRepository<ReportDocument, Long> {

    List<ReportDocument> findByTenantId(String tenantId);

    List<ReportDocument> findByNameContainingIgnoreCase(String name);

    List<ReportDocument> findByReportType(String reportType);

    List<ReportDocument> findByStatus(String status);

    List<ReportDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    List<ReportDocument> findByTenantIdAndReportType(String tenantId, String reportType);

    List<ReportDocument> findByTenantIdAndStatus(String tenantId, String status);

    List<ReportDocument> findByCreatedBy(Long userId);

    List<ReportDocument> findByIsPublic(Boolean isPublic);
}
