package com.globalsearch.repository.search;

import com.globalsearch.document.CompanyDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataElasticsearchTest
@ActiveProfiles("test")
class CompanySearchRepositoryIntegrationTest {

    @Autowired
    private CompanySearchRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSaveAndFindById() {
        // Given
        CompanyDocument company = CompanyDocument.builder()
                .id(1L)
                .name("Test Company")
                .tenantId("TENANT_001")
                .industry("Technology")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        repository.save(company);
        CompanyDocument found = repository.findById(1L).orElse(null);

        // Then
        assertNotNull(found);
        assertEquals("Test Company", found.getName());
        assertEquals("TENANT_001", found.getTenantId());
    }

    @Test
    void testFindByTenantId() {
        // Given
        CompanyDocument company1 = CompanyDocument.builder()
                .id(1L)
                .name("Company 1")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompanyDocument company2 = CompanyDocument.builder()
                .id(2L)
                .name("Company 2")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompanyDocument company3 = CompanyDocument.builder()
                .id(3L)
                .name("Company 3")
                .tenantId("TENANT_B")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(company1, company2, company3));

        // When
        List<CompanyDocument> tenantACompanies = repository.findByTenantId("TENANT_A");
        List<CompanyDocument> tenantBCompanies = repository.findByTenantId("TENANT_B");

        // Then
        assertEquals(2, tenantACompanies.size());
        assertEquals(1, tenantBCompanies.size());
        assertTrue(tenantACompanies.stream().allMatch(c -> c.getTenantId().equals("TENANT_A")));
    }

    @Test
    void testFindByNameContainingIgnoreCase() {
        // Given
        CompanyDocument company1 = CompanyDocument.builder()
                .id(1L)
                .name("Tech Solutions Inc")
                .tenantId("TENANT_001")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompanyDocument company2 = CompanyDocument.builder()
                .id(2L)
                .name("Healthcare Technologies")
                .tenantId("TENANT_002")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(company1, company2));

        // When
        List<CompanyDocument> results = repository.findByNameContainingIgnoreCase("tech");

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Tech Solutions Inc")));
        assertTrue(results.stream().anyMatch(c -> c.getName().equals("Healthcare Technologies")));
    }

    @Test
    void testFindByTenantIdAndNameContainingIgnoreCase() {
        // Given
        CompanyDocument company1 = CompanyDocument.builder()
                .id(1L)
                .name("Tech Corp")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompanyDocument company2 = CompanyDocument.builder()
                .id(2L)
                .name("Tech Industries")
                .tenantId("TENANT_B")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(company1, company2));

        // When
        List<CompanyDocument> results = repository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "tech");

        // Then
        assertEquals(1, results.size());
        assertEquals("Tech Corp", results.get(0).getName());
        assertEquals("TENANT_A", results.get(0).getTenantId());
    }

    @Test
    void testFindByStatus() {
        // Given
        CompanyDocument activeCompany = CompanyDocument.builder()
                .id(1L)
                .name("Active Company")
                .tenantId("TENANT_001")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CompanyDocument inactiveCompany = CompanyDocument.builder()
                .id(2L)
                .name("Inactive Company")
                .tenantId("TENANT_002")
                .status("INACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.saveAll(List.of(activeCompany, inactiveCompany));

        // When
        List<CompanyDocument> activeResults = repository.findByStatus("ACTIVE");
        List<CompanyDocument> inactiveResults = repository.findByStatus("INACTIVE");

        // Then
        assertEquals(1, activeResults.size());
        assertEquals("Active Company", activeResults.get(0).getName());
        assertEquals(1, inactiveResults.size());
        assertEquals("Inactive Company", inactiveResults.get(0).getName());
    }

    @Test
    void testDeleteAll() {
        // Given
        CompanyDocument company = CompanyDocument.builder()
                .id(1L)
                .name("Test Company")
                .tenantId("TENANT_001")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        repository.save(company);
        assertEquals(1, repository.count());

        // When
        repository.deleteAll();

        // Then
        assertEquals(0, repository.count());
    }
}
