package com.globalsearch.repository.search;

import com.globalsearch.document.ZoneDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneSearchRepository extends ElasticsearchRepository<ZoneDocument, Long> {

    List<ZoneDocument> findByTenantId(String tenantId);

    List<ZoneDocument> findByCompanyId(Long companyId);

    List<ZoneDocument> findByLocationId(Long locationId);

    List<ZoneDocument> findByNameContainingIgnoreCase(String name);

    List<ZoneDocument> findByType(String type);

    List<ZoneDocument> findByStatus(String status);

    List<ZoneDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    List<ZoneDocument> findByTenantIdAndCompanyId(String tenantId, Long companyId);
}
