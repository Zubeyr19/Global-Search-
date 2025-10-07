package com.globalsearch.repository;

import com.globalsearch.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findByLocationId(Long locationId);

    Page<Zone> findByLocationId(Long locationId, Pageable pageable);

    @Query("SELECT z FROM Zone z WHERE z.location.company.tenantId = :tenantId")
    List<Zone> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT z FROM Zone z WHERE z.location.id = :locationId AND z.status = :status")
    List<Zone> findByLocationIdAndStatus(@Param("locationId") Long locationId,
                                         @Param("status") Zone.ZoneStatus status);

    @Query("SELECT z FROM Zone z WHERE z.location.company.tenantId = :tenantId AND " +
            "(LOWER(z.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(z.type) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Zone> searchZonesByTenant(@Param("tenantId") String tenantId,
                                   @Param("searchTerm") String searchTerm,
                                   Pageable pageable);

    @Query("SELECT COUNT(z) FROM Zone z WHERE z.location.id = :locationId")
    long countByLocationId(@Param("locationId") Long locationId);
}