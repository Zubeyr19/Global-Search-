package com.globalsearch.repository;

import com.globalsearch.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByTenantId(String tenantId);

    Page<Report> findByTenantId(String tenantId, Pageable pageable);

    List<Report> findByCreatedBy(Long userId);

    Page<Report> findByCreatedBy(Long userId, Pageable pageable);

    List<Report> findByStatus(Report.ReportStatus status);

    List<Report> findByTenantIdAndStatus(String tenantId, Report.ReportStatus status);

    @Query("SELECT r FROM Report r WHERE r.tenantId = :tenantId AND r.reportType = :reportType")
    List<Report> findByTenantIdAndReportType(@Param("tenantId") String tenantId,
                                              @Param("reportType") String reportType);

    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<Report> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.createdBy = :userId")
    Long countByCreatedBy(@Param("userId") Long userId);

    // Delete expired reports
    void deleteByExpiresAtBefore(LocalDateTime date);
}
