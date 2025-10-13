package com.globalsearch.repository.search;

import com.globalsearch.document.LocationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationSearchRepository extends ElasticsearchRepository<LocationDocument, Long> {

    List<LocationDocument> findByTenantId(String tenantId);

    List<LocationDocument> findByCompanyId(Long companyId);

    List<LocationDocument> findByNameContainingIgnoreCase(String name);

    List<LocationDocument> findByCity(String city);

    List<LocationDocument> findByCountry(String country);

    List<LocationDocument> findByStatus(String status);

    List<LocationDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    List<LocationDocument> findByTenantIdAndCompanyId(String tenantId, Long companyId);
}
