package com.globalsearch.service;

import com.globalsearch.document.DashboardDocument;
import com.globalsearch.entity.Dashboard;
import com.globalsearch.repository.DashboardRepository;
import com.globalsearch.repository.search.DashboardSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final DashboardSearchRepository dashboardSearchRepository;

    /**
     * Create a new dashboard
     */
    @Transactional
    public Dashboard createDashboard(Dashboard dashboard) {
        log.info("Creating new dashboard: {}", dashboard.getName());

        if (dashboard.getCreatedAt() == null) {
            dashboard.setCreatedAt(LocalDateTime.now());
        }
        if (dashboard.getUpdatedAt() == null) {
            dashboard.setUpdatedAt(LocalDateTime.now());
        }

        // Save to MySQL
        Dashboard savedDashboard = dashboardRepository.save(dashboard);

        // Index in Elasticsearch
        DashboardDocument document = DashboardDocument.fromEntity(savedDashboard);
        dashboardSearchRepository.save(document);

        log.info("Dashboard created successfully with ID: {}", savedDashboard.getId());
        return savedDashboard;
    }

    /**
     * Get dashboard by ID
     */
    public Dashboard getDashboardById(Long id) {
        log.info("Fetching dashboard with ID: {}", id);
        Dashboard dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dashboard not found: " + id));

        // Increment access count
        dashboard.incrementAccessCount();
        dashboardRepository.save(dashboard);

        return dashboard;
    }

    /**
     * Get all dashboards for a tenant
     */
    public List<Dashboard> getDashboardsByTenant(String tenantId) {
        log.info("Fetching dashboards for tenant: {}", tenantId);
        return dashboardRepository.findByTenantId(tenantId);
    }

    /**
     * Get dashboards by tenant with pagination
     */
    public Page<Dashboard> getDashboardsByTenantPaginated(String tenantId, Pageable pageable) {
        log.info("Fetching paginated dashboards for tenant: {}", tenantId);
        return dashboardRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get dashboards by owner
     */
    public List<Dashboard> getDashboardsByOwner(Long ownerId) {
        log.info("Fetching dashboards owned by user: {}", ownerId);
        return dashboardRepository.findByOwnerId(ownerId);
    }

    /**
     * Get dashboards by owner with pagination
     */
    public Page<Dashboard> getDashboardsByOwnerPaginated(Long ownerId, Pageable pageable) {
        log.info("Fetching paginated dashboards for owner: {}", ownerId);
        return dashboardRepository.findByOwnerId(ownerId, pageable);
    }

    /**
     * Get shared dashboards for a tenant
     */
    public List<Dashboard> getSharedDashboards(String tenantId) {
        log.info("Fetching shared dashboards for tenant: {}", tenantId);
        return dashboardRepository.findByTenantIdAndIsShared(tenantId, true);
    }

    /**
     * Get favorite dashboards for a user
     */
    public List<Dashboard> getFavoriteDashboards(Long ownerId) {
        log.info("Fetching favorite dashboards for user: {}", ownerId);
        return dashboardRepository.findByOwnerIdAndIsFavorite(ownerId, true);
    }

    /**
     * Get dashboards by type
     */
    public List<Dashboard> getDashboardsByType(String tenantId, Dashboard.DashboardType type) {
        log.info("Fetching dashboards for tenant {} with type: {}", tenantId, type);
        return dashboardRepository.findByTenantIdAndDashboardType(tenantId, type);
    }

    /**
     * Get default dashboards for a tenant
     */
    public List<Dashboard> getDefaultDashboards(String tenantId) {
        log.info("Fetching default dashboards for tenant: {}", tenantId);
        return dashboardRepository.findDefaultDashboards(tenantId);
    }

    /**
     * Update dashboard
     */
    @Transactional
    public Dashboard updateDashboard(Long id, Dashboard dashboardDetails) {
        log.info("Updating dashboard with ID: {}", id);

        Dashboard dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dashboard not found: " + id));

        // Update fields
        if (dashboardDetails.getName() != null) {
            dashboard.setName(dashboardDetails.getName());
        }
        if (dashboardDetails.getDescription() != null) {
            dashboard.setDescription(dashboardDetails.getDescription());
        }
        if (dashboardDetails.getConfiguration() != null) {
            dashboard.setConfiguration(dashboardDetails.getConfiguration());
        }
        if (dashboardDetails.getLayout() != null) {
            dashboard.setLayout(dashboardDetails.getLayout());
        }
        if (dashboardDetails.getWidgets() != null) {
            dashboard.setWidgets(dashboardDetails.getWidgets());
        }
        if (dashboardDetails.getDashboardType() != null) {
            dashboard.setDashboardType(dashboardDetails.getDashboardType());
        }
        if (dashboardDetails.getIsDefault() != null) {
            dashboard.setIsDefault(dashboardDetails.getIsDefault());
        }
        if (dashboardDetails.getIsShared() != null) {
            dashboard.setIsShared(dashboardDetails.getIsShared());
        }
        if (dashboardDetails.getIsFavorite() != null) {
            dashboard.setIsFavorite(dashboardDetails.getIsFavorite());
        }
        if (dashboardDetails.getRefreshInterval() != null) {
            dashboard.setRefreshInterval(dashboardDetails.getRefreshInterval());
        }
        if (dashboardDetails.getTags() != null) {
            dashboard.setTags(dashboardDetails.getTags());
        }

        dashboard.setUpdatedAt(LocalDateTime.now());

        // Save to MySQL
        Dashboard updatedDashboard = dashboardRepository.save(dashboard);

        // Update Elasticsearch
        DashboardDocument document = DashboardDocument.fromEntity(updatedDashboard);
        dashboardSearchRepository.save(document);

        log.info("Dashboard updated successfully");
        return updatedDashboard;
    }

    /**
     * Delete dashboard
     */
    @Transactional
    public void deleteDashboard(Long id) {
        log.info("Deleting dashboard with ID: {}", id);

        // Delete from MySQL
        dashboardRepository.deleteById(id);

        // Delete from Elasticsearch
        dashboardSearchRepository.deleteById(id);

        log.info("Dashboard deleted successfully");
    }

    /**
     * Search dashboards in Elasticsearch
     */
    public List<DashboardDocument> searchDashboards(String tenantId, String searchTerm) {
        log.info("Searching dashboards for tenant {} with term: {}", tenantId, searchTerm);

        if (searchTerm == null || searchTerm.isEmpty()) {
            return dashboardSearchRepository.findByTenantId(tenantId);
        }

        return dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, searchTerm);
    }

    /**
     * Index all dashboards to Elasticsearch
     */
    @Transactional
    public void indexAllDashboards() {
        log.info("Indexing all dashboards to Elasticsearch");

        List<Dashboard> allDashboards = dashboardRepository.findAll();
        List<DashboardDocument> documents = allDashboards.stream()
                .map(DashboardDocument::fromEntity)
                .collect(Collectors.toList());

        dashboardSearchRepository.saveAll(documents);

        log.info("Indexed {} dashboards to Elasticsearch", documents.size());
    }

    /**
     * Get total dashboard count
     */
    public long getTotalDashboardCount() {
        return dashboardRepository.count();
    }

    /**
     * Get dashboard count by tenant
     */
    public Long getDashboardCountByTenant(String tenantId) {
        return dashboardRepository.countByTenantId(tenantId);
    }

    /**
     * Get dashboard count by owner
     */
    public Long getDashboardCountByOwner(Long ownerId) {
        return dashboardRepository.countByOwnerId(ownerId);
    }
}
