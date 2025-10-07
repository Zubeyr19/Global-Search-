package com.globalsearch.repository;

import com.globalsearch.entity.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    Optional<Sensor> findBySerialNumber(String serialNumber);

    List<Sensor> findByZoneId(Long zoneId);

    Page<Sensor> findByZoneId(Long zoneId, Pageable pageable);

    List<Sensor> findBySensorType(Sensor.SensorType sensorType);

    List<Sensor> findByStatus(Sensor.SensorStatus status);

    @Query("SELECT s FROM Sensor s WHERE s.zone.location.company.tenantId = :tenantId")
    List<Sensor> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT s FROM Sensor s WHERE s.zone.location.company.tenantId = :tenantId")
    Page<Sensor> findByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT s FROM Sensor s WHERE s.zone.location.id = :locationId")
    List<Sensor> findByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT s FROM Sensor s WHERE s.zone.location.company.id = :companyId")
    List<Sensor> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT s FROM Sensor s WHERE s.zone.location.company.tenantId = :tenantId AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.serialNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.manufacturer) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Sensor> searchSensorsByTenant(@Param("tenantId") String tenantId,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

    @Query("SELECT s FROM Sensor s WHERE s.status = 'FAULTY' OR s.status = 'OFFLINE'")
    List<Sensor> findProblematicSensors();

    @Query("SELECT COUNT(s) FROM Sensor s WHERE s.zone.location.company.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(s) FROM Sensor s WHERE s.zone.id = :zoneId")
    long countByZoneId(@Param("zoneId") Long zoneId);
}