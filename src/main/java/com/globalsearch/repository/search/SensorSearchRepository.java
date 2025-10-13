package com.globalsearch.repository.search;

import com.globalsearch.document.SensorDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorSearchRepository extends ElasticsearchRepository<SensorDocument, Long> {

    List<SensorDocument> findByTenantId(String tenantId);

    List<SensorDocument> findByCompanyId(Long companyId);

    List<SensorDocument> findByLocationId(Long locationId);

    List<SensorDocument> findByZoneId(Long zoneId);

    List<SensorDocument> findByNameContainingIgnoreCase(String name);

    List<SensorDocument> findBySerialNumber(String serialNumber);

    List<SensorDocument> findBySensorType(String sensorType);

    List<SensorDocument> findByStatus(String status);

    List<SensorDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    List<SensorDocument> findByTenantIdAndCompanyId(String tenantId, Long companyId);

    List<SensorDocument> findByTenantIdAndSensorType(String tenantId, String sensorType);
}
