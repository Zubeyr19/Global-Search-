package com.globalsearch;

import com.globalsearch.document.CompanyDocument;
import com.globalsearch.entity.Company;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.repository.search.CompanySearchRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to validate indexing latency requirement: <5 minutes from data change to searchable
 * Requirement: U14 - Indexing latency below 5 minutes (Must-Have, 8 story points)
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Indexing latency tests require Elasticsearch to be running. Enable when Elasticsearch is available.")
public class IndexingLatencyTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySearchRepository companySearchRepository;

    @Test
    public void testIndexingLatency() throws InterruptedException {
        System.out.println("=== Starting Indexing Latency Test ===");
        System.out.println("Requirement: Data must be searchable within 5 minutes of creation");

        // 1. Create new company in MySQL
        Company company = new Company();
        company.setName("Latency Test Company " + System.currentTimeMillis());
        company.setTenantId("TEST_TENANT");
        company.setIndustry("Testing");
        company.setStatus(Company.CompanyStatus.ACTIVE);
        company.setMaxUsers(10);
        company.setMaxLocations(5);
        company.setMaxSensors(50);

        long startTime = System.currentTimeMillis();
        System.out.println("Creating company in MySQL at: " + startTime);
        company = companyRepository.save(company);
        System.out.println("Company created with ID: " + company.getId());

        // 2. Wait and check if searchable
        boolean found = false;
        int attempts = 0;
        int maxAttempts = 30; // 30 attempts * 10 sec = 5 min max
        long checkInterval = 10000; // 10 seconds

        System.out.println("Waiting for document to be indexed in Elasticsearch...");

        while (!found && attempts < maxAttempts) {
            Thread.sleep(checkInterval);
            attempts++;

            Optional<CompanyDocument> doc = companySearchRepository.findById(company.getId());
            if (doc.isPresent()) {
                found = true;
                long endTime = System.currentTimeMillis();
                long latencyMillis = endTime - startTime;
                long latencySeconds = latencyMillis / 1000;

                System.out.println("✓ Document found in Elasticsearch!");
                System.out.println("Indexing latency: " + latencySeconds + " seconds (" + latencyMillis + " ms)");
                System.out.println("Attempts taken: " + attempts);

                // Assert latency is less than 5 minutes (300 seconds)
                assertTrue(latencySeconds < 300,
                    "Indexing took " + latencySeconds + " seconds, which exceeds the 5 minute (300 second) requirement!");

                System.out.println("✓ TEST PASSED: Indexing latency is within acceptable limits");

                // Cleanup
                companyRepository.delete(company);
                companySearchRepository.deleteById(company.getId());
                System.out.println("Test data cleaned up");
            } else {
                System.out.println("Attempt " + attempts + "/" + maxAttempts + ": Document not yet indexed...");
            }
        }

        assertTrue(found, "Document was not indexed within 5 minutes!");
        System.out.println("=== Indexing Latency Test Completed ===");
    }
}
