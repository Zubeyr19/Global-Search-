package com.globalsearch.document;

import com.globalsearch.entity.Company;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDocumentTest {

    @Test
    void testFromEntity_Success() {
        // Given
        Company company = Company.builder()
                .id(1L)
                .name("Test Company")
                .tenantId("TENANT_TEST")
                .industry("Technology")
                .description("A test company")
                .contactEmail("test@company.com")
                .contactPhone("+1234567890")
                .address("123 Test St")
                .city("Test City")
                .state("Test State")
                .country("Test Country")
                .postalCode("12345")
                .status(Company.CompanyStatus.ACTIVE)
                .maxUsers(10)
                .maxLocations(5)
                .maxSensors(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        CompanyDocument document = CompanyDocument.fromEntity(company);

        // Then
        assertNotNull(document);
        assertEquals(company.getId(), document.getId());
        assertEquals(company.getName(), document.getName());
        assertEquals(company.getTenantId(), document.getTenantId());
        assertEquals(company.getIndustry(), document.getIndustry());
        assertEquals(company.getDescription(), document.getDescription());
        assertEquals(company.getContactEmail(), document.getContactEmail());
        assertEquals(company.getContactPhone(), document.getContactPhone());
        assertEquals(company.getAddress(), document.getAddress());
        assertEquals(company.getCity(), document.getCity());
        assertEquals(company.getState(), document.getState());
        assertEquals(company.getCountry(), document.getCountry());
        assertEquals(company.getPostalCode(), document.getPostalCode());
        assertEquals(company.getStatus().name(), document.getStatus());
        assertEquals(company.getMaxUsers(), document.getMaxUsers());
        assertEquals(company.getMaxLocations(), document.getMaxLocations());
        assertEquals(company.getMaxSensors(), document.getMaxSensors());
        assertEquals(company.getCreatedAt(), document.getCreatedAt());
        assertEquals(company.getUpdatedAt(), document.getUpdatedAt());
    }

    @Test
    void testFromEntity_WithNullOptionalFields() {
        // Given
        Company company = Company.builder()
                .id(1L)
                .name("Minimal Company")
                .tenantId("TENANT_MINIMAL")
                .status(Company.CompanyStatus.ACTIVE)
                .maxUsers(10)
                .maxLocations(5)
                .maxSensors(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        CompanyDocument document = CompanyDocument.fromEntity(company);

        // Then
        assertNotNull(document);
        assertEquals(company.getId(), document.getId());
        assertEquals(company.getName(), document.getName());
        assertEquals(company.getTenantId(), document.getTenantId());
        assertNull(document.getIndustry());
        assertNull(document.getDescription());
    }

    @Test
    void testBuilder() {
        // When
        CompanyDocument document = CompanyDocument.builder()
                .id(1L)
                .name("Builder Company")
                .tenantId("TENANT_BUILDER")
                .status("ACTIVE")
                .build();

        // Then
        assertNotNull(document);
        assertEquals(1L, document.getId());
        assertEquals("Builder Company", document.getName());
        assertEquals("TENANT_BUILDER", document.getTenantId());
        assertEquals("ACTIVE", document.getStatus());
    }
}
