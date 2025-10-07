package com.globalsearch.repository;

import com.globalsearch.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByTenantId(String tenantId);

    Optional<Company> findByName(String name);

    boolean existsByTenantId(String tenantId);

    boolean existsByName(String name);

    Page<Company> findByStatus(Company.CompanyStatus status, Pageable pageable);

    @Query("SELECT c FROM Company c WHERE c.status = 'ACTIVE'")
    List<Company> findAllActiveCompanies();

    @Query("SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Company> searchCompanies(@Param("searchTerm") String searchTerm, Pageable pageable);
}
