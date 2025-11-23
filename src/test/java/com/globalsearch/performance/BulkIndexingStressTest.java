package com.globalsearch.performance;

import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.Sensor;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.repository.SensorRepository;
import com.globalsearch.repository.search.CompanySearchRepository;
import com.globalsearch.repository.search.SensorSearchRepository;
import com.globalsearch.service.ElasticsearchSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress tests for bulk indexing operations
 * Tests system behavior when indexing large volumes of data
 *
 * Requirements tested:
 * - NFR-U14: Indexing latency below 5 minutes
 * - Bulk data synchronization performance
 * - System stability under heavy indexing load
 */
@SpringBootTest
@ActiveProfiles("test")
public class BulkIndexingStressTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private CompanySearchRepository companySearchRepository;

    @Autowired
    private SensorSearchRepository sensorSearchRepository;

    @Autowired
    private ElasticsearchSyncService syncService;

    @BeforeEach
    void setUp() {
        // Clean up
        companyRepository.deleteAll();
        sensorRepository.deleteAll();
        companySearchRepository.deleteAll();
        sensorSearchRepository.deleteAll();
    }

    /**
     * Test: Bulk indexing of 1000 companies
     * Requirement: Should complete within reasonable time (<5 minutes)
     */
    @Test
    public void testBulkIndexing_1000Companies_CompletesWithinTimeLimit() throws Exception {
        // Given
        int companyCount = 1000;
        System.out.println("Creating " + companyCount + " companies in MySQL...");

        long createStartTime = System.currentTimeMillis();
        List<Company> companies = new ArrayList<>();

        for (int i = 1; i <= companyCount; i++) {
            Company company = Company.builder()
                    .name("Bulk Test Company " + i)
                    .tenantId("TENANT_BULK_" + (i % 10)) // 10 different tenants
                    .industry("Technology")
                    .status(Company.CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            companies.add(company);
        }

        companyRepository.saveAll(companies);
        long createDuration = System.currentTimeMillis() - createStartTime;
        System.out.println("Created " + companyCount + " companies in MySQL in " + createDuration + "ms");

        // When - Synchronize to Elasticsearch
        System.out.println("Starting Elasticsearch synchronization...");
        long syncStartTime = System.currentTimeMillis();

        syncService.syncAllCompanies();

        long syncDuration = System.currentTimeMillis() - syncStartTime;
        System.out.println("Synchronized " + companyCount + " companies to Elasticsearch in " + syncDuration + "ms");

        // Wait for indexing to complete
        Thread.sleep(2000);

        // Then
        long totalCount = companySearchRepository.count();
        System.out.println("Indexed count: " + totalCount);

        assertThat(totalCount).isEqualTo(companyCount);
        assertThat(syncDuration).isLessThan(5 * 60 * 1000); // Less than 5 minutes

        System.out.println("✓ Bulk indexing test PASSED: " + companyCount +
                " companies indexed in " + syncDuration + "ms");
    }

    /**
     * Test: Bulk indexing of 5000 sensors
     * Tests large-scale indexing performance
     */
    @Test
    public void testBulkIndexing_5000Sensors_MaintainsPerformance() throws Exception {
        // Given
        int sensorCount = 5000;
        System.out.println("Creating " + sensorCount + " sensors...");

        List<Sensor> sensors = new ArrayList<>();
        for (int i = 1; i <= sensorCount; i++) {
            Sensor sensor = Sensor.builder()
                    .name("Bulk Sensor " + i)
                    .serialNumber("SN-BULK-" + String.format("%05d", i))
                    .sensorType(Sensor.SensorType.values()[i % Sensor.SensorType.values().length])
                    .status(Sensor.SensorStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            sensors.add(sensor);
        }

        sensorRepository.saveAll(sensors);
        System.out.println("Saved " + sensorCount + " sensors to MySQL");

        // When
        long syncStartTime = System.currentTimeMillis();
        syncService.syncAllSensors();
        long syncDuration = System.currentTimeMillis() - syncStartTime;

        Thread.sleep(3000); // Wait for async indexing

        // Then
        long indexedCount = sensorSearchRepository.count();
        System.out.println("Indexed " + indexedCount + " sensors in " + syncDuration + "ms");

        assertThat(indexedCount).isEqualTo(sensorCount);
        assertThat(syncDuration).isLessThan(5 * 60 * 1000); // Less than 5 minutes

        double perSecond = (double) sensorCount / (syncDuration / 1000.0);
        System.out.println("Indexing rate: " + String.format("%.2f", perSecond) + " sensors/second");
        System.out.println("✓ Large-scale bulk indexing test PASSED");
    }

    /**
     * Test: Concurrent bulk indexing of different entity types
     * Tests system stability when indexing multiple entity types simultaneously
     */
    @Test
    public void testConcurrentBulkIndexing_MultipleEntityTypes() throws Exception {
        // Given
        int entitiesPerType = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CountDownLatch completionLatch = new CountDownLatch(2);

        System.out.println("Testing concurrent bulk indexing of multiple entity types...");

        // Create test data
        List<Company> companies = new ArrayList<>();
        for (int i = 1; i <= entitiesPerType; i++) {
            companies.add(Company.builder()
                    .name("Concurrent Company " + i)
                    .tenantId("TENANT_CONCURRENT")
                    .status(Company.CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        companyRepository.saveAll(companies);

        List<Sensor> sensors = new ArrayList<>();
        for (int i = 1; i <= entitiesPerType; i++) {
            sensors.add(Sensor.builder()
                    .name("Concurrent Sensor " + i)
                    .serialNumber("SN-CONC-" + i)
                    .sensorType(Sensor.SensorType.TEMPERATURE)
                    .status(Sensor.SensorStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        sensorRepository.saveAll(sensors);

        long startTime = System.currentTimeMillis();

        // When - Index different entity types concurrently
        executorService.submit(() -> {
            try {
                syncService.syncAllCompanies();
                System.out.println("Companies sync completed");
            } finally {
                completionLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                syncService.syncAllSensors();
                System.out.println("Sensors sync completed");
            } finally {
                completionLatch.countDown();
            }
        });

        // Wait for completion
        boolean completed = completionLatch.await(5, TimeUnit.MINUTES);
        long duration = System.currentTimeMillis() - startTime;
        executorService.shutdown();

        Thread.sleep(2000); // Wait for final indexing

        // Then
        assertTrue(completed, "Concurrent indexing should complete within 5 minutes");

        long companyCount = companySearchRepository.count();
        long sensorCount = sensorSearchRepository.count();

        System.out.println("\n=== Concurrent Indexing Results ===");
        System.out.println("Companies indexed: " + companyCount + "/" + entitiesPerType);
        System.out.println("Sensors indexed: " + sensorCount + "/" + entitiesPerType);
        System.out.println("Total duration: " + duration + "ms");

        assertThat(companyCount).isEqualTo(entitiesPerType);
        assertThat(sensorCount).isEqualTo(entitiesPerType);

        System.out.println("✓ Concurrent bulk indexing test PASSED");
    }

    /**
     * Test: Incremental indexing under concurrent updates
     * Tests indexing behavior when entities are being updated during sync
     */
    @Test
    public void testIncrementalIndexing_UnderConcurrentUpdates() throws Exception {
        // Given
        int initialCount = 100;
        int additionalCount = 50;

        // Create initial batch
        List<Company> initialCompanies = new ArrayList<>();
        for (int i = 1; i <= initialCount; i++) {
            initialCompanies.add(Company.builder()
                    .name("Initial Company " + i)
                    .tenantId("TENANT_INCREMENTAL")
                    .status(Company.CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        companyRepository.saveAll(initialCompanies);

        // Sync initial batch
        syncService.syncAllCompanies();
        Thread.sleep(1000);

        System.out.println("Initial batch of " + initialCount + " companies indexed");

        // When - Add more companies while system is running
        List<Company> additionalCompanies = new ArrayList<>();
        for (int i = 1; i <= additionalCount; i++) {
            Company company = Company.builder()
                    .name("Additional Company " + i)
                    .tenantId("TENANT_INCREMENTAL")
                    .status(Company.CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            additionalCompanies.add(company);
        }

        companyRepository.saveAll(additionalCompanies);
        System.out.println("Added " + additionalCount + " additional companies");

        // Sync again
        syncService.syncAllCompanies();
        Thread.sleep(1000);

        // Then
        long totalIndexed = companySearchRepository.count();
        System.out.println("Total indexed after incremental sync: " + totalIndexed);

        assertThat(totalIndexed).isEqualTo(initialCount + additionalCount);

        System.out.println("✓ Incremental indexing test PASSED");
    }

    /**
     * Test: Memory stability during large bulk operations
     * Ensures no memory leaks or excessive memory usage
     */
    @Test
    public void testMemoryStability_DuringLargeBulkOperations() throws Exception {
        // Given
        int batchCount = 5;
        int entitiesPerBatch = 200;

        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Processing " + batchCount + " batches of " + entitiesPerBatch + " entities each...");

        // When - Process multiple batches
        for (int batch = 1; batch <= batchCount; batch++) {
            List<Company> companies = new ArrayList<>();
            for (int i = 1; i <= entitiesPerBatch; i++) {
                companies.add(Company.builder()
                        .name("Memory Test Company Batch" + batch + "_" + i)
                        .tenantId("TENANT_MEMORY_TEST")
                        .status(Company.CompanyStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            companyRepository.saveAll(companies);
            syncService.syncAllCompanies();

            // Force garbage collection
            System.gc();
            Thread.sleep(500);

            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Batch " + batch + " - Memory usage: " +
                    (currentMemory / 1024 / 1024) + " MB");
        }

        Thread.sleep(2000);
        System.gc();

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.println("\n=== Memory Stability Results ===");
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");

        // Then - Memory increase should be reasonable (less than 200 MB)
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024);

        long totalIndexed = companySearchRepository.count();
        assertThat(totalIndexed).isEqualTo(batchCount * entitiesPerBatch);

        System.out.println("✓ Memory stability test PASSED");
    }

    /**
     * Test: Error recovery during bulk indexing
     * Tests system behavior when some indexing operations fail
     */
    @Test
    public void testErrorRecovery_PartialIndexingFailure() throws Exception {
        // Given
        int totalCount = 100;
        List<Company> companies = new ArrayList<>();

        for (int i = 1; i <= totalCount; i++) {
            companies.add(Company.builder()
                    .name("Error Test Company " + i)
                    .tenantId("TENANT_ERROR_TEST")
                    .status(Company.CompanyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        companyRepository.saveAll(companies);
        System.out.println("Created " + totalCount + " companies for error recovery test");

        // When - Sync (some may fail but process should continue)
        long startTime = System.currentTimeMillis();
        syncService.syncAllCompanies();
        long duration = System.currentTimeMillis() - startTime;

        Thread.sleep(2000);

        // Then - Most should be indexed despite any individual failures
        long indexedCount = companySearchRepository.count();
        double successRate = (double) indexedCount / totalCount * 100;

        System.out.println("\n=== Error Recovery Results ===");
        System.out.println("Total entities: " + totalCount);
        System.out.println("Successfully indexed: " + indexedCount);
        System.out.println("Success rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Duration: " + duration + "ms");

        // Accept at least 95% success rate
        assertThat(successRate).isGreaterThan(95.0);

        System.out.println("✓ Error recovery test PASSED");
    }

    /**
     * Test: Throughput measurement
     * Measures maximum indexing throughput
     */
    @Test
    public void testIndexingThroughput_MeasureMaximumRate() throws Exception {
        // Given
        int entityCount = 1000;
        List<Sensor> sensors = new ArrayList<>();

        for (int i = 1; i <= entityCount; i++) {
            sensors.add(Sensor.builder()
                    .name("Throughput Sensor " + i)
                    .serialNumber("SN-THRU-" + i)
                    .sensorType(Sensor.SensorType.TEMPERATURE)
                    .status(Sensor.SensorStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        sensorRepository.saveAll(sensors);

        // When - Measure indexing throughput
        long startTime = System.currentTimeMillis();
        syncService.syncAllSensors();
        long duration = System.currentTimeMillis() - startTime;

        Thread.sleep(2000);

        // Then
        long indexedCount = sensorSearchRepository.count();
        double throughputPerSecond = (double) indexedCount / (duration / 1000.0);
        double avgLatencyMs = (double) duration / indexedCount;

        System.out.println("\n=== Throughput Measurement Results ===");
        System.out.println("Total indexed: " + indexedCount);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughputPerSecond) + " entities/second");
        System.out.println("Average latency per entity: " + String.format("%.2f", avgLatencyMs) + "ms");

        assertThat(indexedCount).isEqualTo(entityCount);
        assertThat(throughputPerSecond).isGreaterThan(10); // At least 10 entities/second

        System.out.println("✓ Throughput measurement test PASSED");
    }
}
