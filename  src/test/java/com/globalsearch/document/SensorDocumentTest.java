package com.globalsearch.document;

import com.globalsearch.entity.Company;
import com.globalsearch.entity.Location;
import com.globalsearch.entity.Sensor;
import com.globalsearch.entity.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SensorDocumentTest {

    @Test
    void testFromEntity_WithFullHierarchy() {
        // Given
        Company company = Company.builder()
                .id(1L)
                .tenantId("TENANT_TEST")
                .build();

        Location location = Location.builder()
                .id(10L)
                .company(company)
                .build();

        Zone zone = Zone.builder()
                .id(100L)
                .location(location)
                .build();

        Sensor sensor = Sensor.builder()
                .id(1000L)
                .name("Temperature Sensor 1")
                .serialNumber("SN-12345")
                .sensorType(Sensor.SensorType.TEMPERATURE)
                .manufacturer("TestCorp")
                .model("TC-100")
                .description("Test sensor")
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(25.5)
                .unitOfMeasurement("°C")
                .batteryLevel(80)
                .zone(zone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        SensorDocument document = SensorDocument.fromEntity(sensor);

        // Then
        assertNotNull(document);
        assertEquals(sensor.getId(), document.getId());
        assertEquals(sensor.getName(), document.getName());
        assertEquals(sensor.getSerialNumber(), document.getSerialNumber());
        assertEquals(sensor.getSensorType().name(), document.getSensorType());
        assertEquals(sensor.getStatus().name(), document.getStatus());

        // Security fields
        assertEquals(zone.getId(), document.getZoneId());
        assertEquals(location.getId(), document.getLocationId());
        assertEquals(company.getId(), document.getCompanyId());
        assertEquals(company.getTenantId(), document.getTenantId());
    }

    @Test
    void testFromEntity_WithNullZone() {
        // Given
        Sensor sensor = Sensor.builder()
                .id(1000L)
                .name("Orphan Sensor")
                .serialNumber("SN-ORPHAN")
                .sensorType(Sensor.SensorType.HUMIDITY)
                .status(Sensor.SensorStatus.INACTIVE)
                .zone(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        SensorDocument document = SensorDocument.fromEntity(sensor);

        // Then
        assertNotNull(document);
        assertNull(document.getZoneId());
        assertNull(document.getLocationId());
        assertNull(document.getCompanyId());
        assertNull(document.getTenantId());
    }

    @Test
    void testFromEntity_SecurityFieldsPropagation() {
        // Given - Testing that tenantId propagates correctly through hierarchy
        Company company = Company.builder()
                .id(5L)
                .tenantId("TENANT_SECURITY_TEST")
                .build();

        Location location = Location.builder()
                .id(50L)
                .company(company)
                .build();

        Zone zone = Zone.builder()
                .id(500L)
                .location(location)
                .build();

        Sensor sensor = Sensor.builder()
                .id(5000L)
                .name("Security Test Sensor")
                .serialNumber("SN-SEC-001")
                .sensorType(Sensor.SensorType.MOTION)
                .status(Sensor.SensorStatus.ACTIVE)
                .zone(zone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        SensorDocument document = SensorDocument.fromEntity(sensor);

        // Then - Verify security fields are correctly populated
        assertEquals("TENANT_SECURITY_TEST", document.getTenantId());
        assertEquals(5L, document.getCompanyId());
        assertEquals(50L, document.getLocationId());
        assertEquals(500L, document.getZoneId());
    }
}
