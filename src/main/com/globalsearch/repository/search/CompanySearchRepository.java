package com.globalsearch.repository.search;

import com.globalsearch.document.CompanyDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanySearchRepository extends ElasticsearchRepository<CompanyDocument, Long> {

    List<CompanyDocument> findByTenantId(String tenantId);

    List<CompanyDocument> findByNameContainingIgnoreCase(String name);

    List<CompanyDocument> findByIndustryContainingIgnoreCase(String industry);

    List<CompanyDocument> findByStatus(String status);

    List<CompanyDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);
}
