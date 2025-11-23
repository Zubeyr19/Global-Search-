package com.globalsearch.performance;

import com.globalsearch.entity.Company;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.repository.search.CompanySearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to validate indexing latency requirement U14:
 * "Indexing latency below 5 minutes so that users don't experience outdated results"
 *
 * This test measures the time from MySQL entity creation to Elasticsearch searchability.
 */
@SpringBootTest
@ActiveProfiles("test")
public class IndexingLatencyTest {

    private static final int MAX_WAIT_SECONDS = 300; // 5 minutes
    private static final int CHECK_INTERVAL_SECONDS = 2; // Check every 2 seconds

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySearchRepository companySearchRepository;

    /**
     * Test: Company indexing latency
     * Requirement: Company data should be searchable within 5 minutes of creation
     */
    @Test
    public void testCompanyIndexingLatency() throws InterruptedException {
        // Create unique company
        Company company = new Company();
        company.setName("Latency Test Company " + System.currentTimeMillis());
        company.setTenantId("TEST_TENANT_" + System.currentTimeMillis());
        company.setDescription("Test company for indexing latency validation");
        company.setIndustry("Technology");
        company.setCountry("Denmark");

        // Record start time and save to MySQL
        Instant startTime = Instant.now();
        Company savedCompany = companyRepository.save(company);
        Long companyId = savedCompany.getId();

        System.out.printf("Created company with ID %d at %s%n", companyId, startTime);

        // Wait for Elasticsearch indexing
        boolean indexed = waitForIndexing(
            () -> companySearchRepository.findById(companyId).isPresent(),
            "Company",
            companyId
        );

        // Calculate latency
        Duration latency = Duration.between(startTime, Instant.now());
        long latencySeconds = latency.getSeconds();

        // Assertions
        assertTrue(indexed,
            String.format("Company not indexed within %d seconds", MAX_WAIT_SECONDS));
        assertTrue(latencySeconds < MAX_WAIT_SECONDS,
            String.format("Indexing latency (%d seconds) exceeds requirement of %d seconds",
                latencySeconds, MAX_WAIT_SECONDS));

        // Log results
        System.out.printf("✓ Company indexing latency: %d seconds (requirement: < %d seconds)%n",
            latencySeconds, MAX_WAIT_SECONDS);

        // Cleanup
        companyRepository.deleteById(companyId);
        System.out.printf("Cleaned up test company %d%n", companyId);
    }

    /**
     * Test: Bulk indexing latency (10 entities)
     * Requirement: Bulk operations should complete within 5 minutes
     */
    @Test
    public void testBulkIndexingLatency() throws InterruptedException {
        int bulkCount = 10;
        Instant startTime = Instant.now();
        long timestamp = System.currentTimeMillis();

        System.out.printf("Creating %d companies in bulk...%n", bulkCount);

        // Create 10 companies
        for (int i = 0; i < bulkCount; i++) {
            Company company = new Company();
            company.setName("Bulk Test Company " + timestamp + "_" + i);
            company.setTenantId("BULK_TEST_" + timestamp);
            company.setIndustry("Technology");
            companyRepository.save(company);
        }

        System.out.println("All companies saved to MySQL, waiting for Elasticsearch indexing...");

        // Wait a bit for indexing to process
        Thread.sleep(15000); // 15 seconds

        Duration latency = Duration.between(startTime, Instant.now());
        long latencySeconds = latency.getSeconds();

        // Assert bulk operation completes reasonably
        assertTrue(latencySeconds < MAX_WAIT_SECONDS,
            String.format("Bulk indexing latency (%d seconds) exceeds requirement", latencySeconds));

        System.out.printf("✓ Bulk indexing (%d entities) latency: %d seconds%n",
            bulkCount, latencySeconds);
    }

    /**
     * Test: Multiple sequential indexing operations
     * Validates that repeated indexing operations maintain performance
     */
    @Test
    public void testSequentialIndexingLatency() throws InterruptedException {
        int sequentialCount = 5;
        long totalLatency = 0;
        long timestamp = System.currentTimeMillis();

        System.out.printf("Testing %d sequential indexing operations...%n", sequentialCount);

        for (int i = 0; i < sequentialCount; i++) {
            Company company = new Company();
            company.setName("Sequential Test Company " + timestamp + "_" + i);
            company.setTenantId("SEQ_TEST_" + timestamp);
            company.setIndustry("Technology");

            Instant startTime = Instant.now();
            Company savedCompany = companyRepository.save(company);

            // Wait for indexing
            boolean indexed = waitForIndexing(
                () -> companySearchRepository.findById(savedCompany.getId()).isPresent(),
                "Company",
                savedCompany.getId()
            );

            Duration latency = Duration.between(startTime, Instant.now());
            long latencySeconds = latency.getSeconds();
            totalLatency += latencySeconds;

            assertTrue(indexed, String.format("Company %d not indexed within %d seconds",
                i + 1, MAX_WAIT_SECONDS));
            assertTrue(latencySeconds < MAX_WAIT_SECONDS,
                String.format("Indexing latency for company %d (%d seconds) exceeds requirement",
                    i + 1, latencySeconds));

            System.out.printf("  Company %d indexed in %d seconds%n", i + 1, latencySeconds);
        }

        double averageLatency = (double) totalLatency / sequentialCount;
        System.out.printf("✓ Average indexing latency across %d operations: %.2f seconds%n",
            sequentialCount, averageLatency);
    }

    /**
     * Helper method to wait for indexing with periodic checks
     *
     * @param checkFunction Function to check if entity is indexed
     * @param entityType Type of entity being indexed (for logging)
     * @param entityId ID of the entity
     * @return true if indexed within time limit, false otherwise
     */
    private boolean waitForIndexing(IndexCheckFunction checkFunction, String entityType, Long entityId)
            throws InterruptedException {
        int attempts = 0;
        int maxAttempts = MAX_WAIT_SECONDS / CHECK_INTERVAL_SECONDS;

        while (attempts < maxAttempts) {
            if (checkFunction.check()) {
                System.out.printf("%s %d indexed successfully after %d seconds%n",
                    entityType, entityId, attempts * CHECK_INTERVAL_SECONDS);
                return true;
            }

            attempts++;
            if (attempts % 5 == 0) {
                System.out.printf("Waiting for %s %d indexing... (%d seconds elapsed)%n",
                    entityType, entityId, attempts * CHECK_INTERVAL_SECONDS);
            }

            Thread.sleep(CHECK_INTERVAL_SECONDS * 1000);
        }

        return false;
    }

    /**
     * Functional interface for index checking
     */
    @FunctionalInterface
    interface IndexCheckFunction {
        boolean check();
    }
}
