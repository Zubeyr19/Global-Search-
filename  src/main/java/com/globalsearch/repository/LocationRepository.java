package com.globalsearch.repository;

import com.globalsearch.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByCompanyId(Long companyId);

    Page<Location> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT l FROM Location l WHERE l.company.tenantId = :tenantId")
    List<Location> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT l FROM Location l WHERE l.company.id = :companyId AND l.status = :status")
    List<Location> findByCompanyIdAndStatus(@Param("companyId") Long companyId,
                                            @Param("status") Location.LocationStatus status);

    @Query("SELECT l FROM Location l WHERE l.company.tenantId = :tenantId AND " +
            "(LOWER(l.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(l.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(l.type) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Location> searchLocationsByTenant(@Param("tenantId") String tenantId,
                                           @Param("searchTerm") String searchTerm,
                                           Pageable pageable);

    @Query("SELECT COUNT(l) FROM Location l WHERE l.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);
}