package com.globalsearch.repository.search;

import com.globalsearch.document.DashboardDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardSearchRepository extends ElasticsearchRepository<DashboardDocument, Long> {

    List<DashboardDocument> findByTenantId(String tenantId);

    List<DashboardDocument> findByNameContainingIgnoreCase(String name);

    List<DashboardDocument> findByDashboardType(String dashboardType);

    List<DashboardDocument> findByOwnerId(Long ownerId);

    List<DashboardDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    List<DashboardDocument> findByTenantIdAndOwnerId(String tenantId, Long ownerId);

    List<DashboardDocument> findByTenantIdAndDashboardType(String tenantId, String dashboardType);

    List<DashboardDocument> findByIsShared(Boolean isShared);

    List<DashboardDocument> findByIsFavorite(Boolean isFavorite);

    List<DashboardDocument> findByIsDefault(Boolean isDefault);

    List<DashboardDocument> findByTenantIdAndIsShared(String tenantId, Boolean isShared);
}
