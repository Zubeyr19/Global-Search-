package com.globalsearch.repository;

import com.globalsearch.entity.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    List<Dashboard> findByTenantId(String tenantId);

    Page<Dashboard> findByTenantId(String tenantId, Pageable pageable);

    List<Dashboard> findByOwnerId(Long ownerId);

    Page<Dashboard> findByOwnerId(Long ownerId, Pageable pageable);

    List<Dashboard> findByTenantIdAndOwnerId(String tenantId, Long ownerId);

    List<Dashboard> findByTenantIdAndIsShared(String tenantId, Boolean isShared);

    List<Dashboard> findByOwnerIdAndIsFavorite(Long ownerId, Boolean isFavorite);

    @Query("SELECT d FROM Dashboard d WHERE d.tenantId = :tenantId AND d.dashboardType = :type")
    List<Dashboard> findByTenantIdAndDashboardType(@Param("tenantId") String tenantId,
                                                     @Param("type") Dashboard.DashboardType type);

    @Query("SELECT d FROM Dashboard d WHERE d.tenantId = :tenantId AND d.isDefault = true")
    List<Dashboard> findDefaultDashboards(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.ownerId = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);
}
