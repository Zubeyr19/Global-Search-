package com.globalsearch.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.request.LoginRequest;
import com.globalsearch.dto.response.LoginResponse;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.User;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.repository.UserRepository;
import com.globalsearch.repository.search.CompanySearchRepository;
import com.globalsearch.repository.search.SensorSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Load tests for concurrent search operations
 * Tests system behavior under concurrent user load
 *
 * Requirements tested:
 * - NFR-U13: Support 100 concurrent users with <500ms response time
 * - System stability under concurrent load
 * - Thread safety and data consistency
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Performance tests require Elasticsearch to be running. Enable when Elasticsearch is available.")
public class ConcurrentSearchLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySearchRepository companySearchRepository;

    @Autowired
    private SensorSearchRepository sensorSearchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private static final int CONCURRENT_USERS = 100;
    private static final int SEARCHES_PER_USER = 10;
    private static final int MAX_RESPONSE_TIME_MS = 500;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        userRepository.deleteAll();
        companyRepository.deleteAll();
        companySearchRepository.deleteAll();
        sensorSearchRepository.deleteAll();

        // Create test company
        Company testCompany = Company.builder()
                .name("Tech Corp")
                .tenantId("TENANT_LOAD_TEST")
                .industry("Technology")
                .status(Company.CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create test user
        User testUser = User.builder()
                .username("loadtestuser")
                .password(passwordEncoder.encode("password123"))
                .email("loadtest@example.com")
                .firstName("Load")
                .lastName("Test")
                .tenantId("TENANT_LOAD_TEST")
                .companyId(testCompany.getId())
                .roles(Set.of(User.Role.VIEWER))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(testUser);

        // Create test data - 50 companies
        for (int i = 1; i <= 50; i++) {
            CompanyDocument company = CompanyDocument.builder()
                    .id((long) i)
                    .name("Company " + i)
                    .tenantId("TENANT_LOAD_TEST")
                    .industry("Technology")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now().toLocalDate())
                    .build();
            companySearchRepository.save(company);
        }

        // Create test sensors - 100 sensors
        for (int i = 1; i <= 100; i++) {
            SensorDocument sensor = SensorDocument.builder()
                    .id((long) i)
                    .name("Sensor " + i)
                    .serialNumber("SN-" + String.format("%03d", i))
                    .sensorType("TEMPERATURE")
                    .status("ACTIVE")
                    .tenantId("TENANT_LOAD_TEST")
                    .build();
            sensorSearchRepository.save(sensor);
        }

        // Wait for indexing
        Thread.sleep(2000);

        // Login to get auth token
        authToken = loginAndGetToken("loadtestuser", "password123");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        return response.getAccessToken();
    }

    /**
     * Test: 100 concurrent users performing searches
     * Requirement: NFR-U13 - 100 concurrent users with <500ms response time
     */
    @Test
    public void testConcurrentSearches_100Users_MeetsResponseTimeRequirement() throws Exception {
        // Given
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        System.out.println("Starting concurrent search test with " + CONCURRENT_USERS + " users...");

        // When - Launch concurrent searches
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executorService.submit(() -> {
                try {
                    // Wait for start signal
                    startLatch.await();

                    // Perform search
                    long startTime = System.currentTimeMillis();

                    GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                            .query("Company")
                            .page(0)
                            .size(20)
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/search")
                                    .header("Authorization", "Bearer " + authToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(searchRequest)))
                            .andExpect(status().isOk())
                            .andReturn();

                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                    successCount.incrementAndGet();

                    if (userId % 10 == 0) {
                        System.out.printf("User %d completed search in %dms%n", userId, responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.printf("User %d failed: %s%n", userId, e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        long totalDuration = System.currentTimeMillis() - testStartTime;

        executorService.shutdown();

        // Then - Analyze results
        assertTrue(completed, "Test should complete within 60 seconds");
        System.out.println("\n=== Load Test Results ===");
        System.out.println("Total users: " + CONCURRENT_USERS);
        System.out.println("Successful searches: " + successCount.get());
        System.out.println("Failed searches: " + failureCount.get());
        System.out.println("Total duration: " + totalDuration + "ms");

        // Calculate statistics
        DoubleSummaryStatistics stats = responseTimes.stream()
                .mapToDouble(Long::doubleValue)
                .summaryStatistics();

        System.out.println("\n=== Response Time Statistics ===");
        System.out.println("Average: " + String.format("%.2f", stats.getAverage()) + "ms");
        System.out.println("Min: " + (long) stats.getMin() + "ms");
        System.out.println("Max: " + (long) stats.getMax() + "ms");
        System.out.println("Total requests: " + stats.getCount());

        // Calculate percentiles
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        Collections.sort(sortedTimes);
        long p50 = sortedTimes.get(sortedTimes.size() / 2);
        long p95 = sortedTimes.get((int) (sortedTimes.size() * 0.95));
        long p99 = sortedTimes.get((int) (sortedTimes.size() * 0.99));

        System.out.println("P50 (median): " + p50 + "ms");
        System.out.println("P95: " + p95 + "ms");
        System.out.println("P99: " + p99 + "ms");

        // Assertions
        assertThat(successCount.get()).isEqualTo(CONCURRENT_USERS);
        assertThat(failureCount.get()).isEqualTo(0);
        assertThat(stats.getAverage()).isLessThan(MAX_RESPONSE_TIME_MS);
        assertThat(p95).isLessThan(MAX_RESPONSE_TIME_MS);

        System.out.println("\n✓ Load test PASSED: All " + CONCURRENT_USERS +
                " concurrent users completed successfully with average response time < " +
                MAX_RESPONSE_TIME_MS + "ms");
    }

    /**
     * Test: Sustained load - 50 users performing multiple searches each
     */
    @Test
    public void testSustainedLoad_MultipleSearchesPerUser() throws Exception {
        // Given
        int concurrentUsers = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch completionLatch = new CountDownLatch(concurrentUsers * SEARCHES_PER_USER);

        AtomicInteger totalSearches = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);

        System.out.println("Starting sustained load test: " + concurrentUsers +
                " users x " + SEARCHES_PER_USER + " searches each...");

        // When - Each user performs multiple searches
        for (int userId = 0; userId < concurrentUsers; userId++) {
            final int user = userId;
            executorService.submit(() -> {
                for (int searchNum = 0; searchNum < SEARCHES_PER_USER; searchNum++) {
                    try {
                        long startTime = System.currentTimeMillis();

                        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                                .query("Sensor " + (searchNum % 10))
                                .page(0)
                                .size(10)
                                .build();

                        mockMvc.perform(post("/api/search")
                                        .header("Authorization", "Bearer " + authToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(searchRequest)))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);
                        totalSearches.incrementAndGet();

                    } catch (Exception e) {
                        errors.incrementAndGet();
                        System.err.printf("User %d search %d failed: %s%n",
                                user, searchNum, e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                }
            });
        }

        // Wait for completion
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertTrue(completed, "Sustained load test should complete within 120 seconds");

        double averageResponseTime = (double) totalResponseTime.get() / totalSearches.get();

        System.out.println("\n=== Sustained Load Test Results ===");
        System.out.println("Total searches: " + totalSearches.get());
        System.out.println("Successful: " + (totalSearches.get() - errors.get()));
        System.out.println("Errors: " + errors.get());
        System.out.println("Average response time: " + String.format("%.2f", averageResponseTime) + "ms");

        assertThat(errors.get()).isLessThan(totalSearches.get() / 100); // Less than 1% error rate
        assertThat(averageResponseTime).isLessThan(MAX_RESPONSE_TIME_MS);

        System.out.println("✓ Sustained load test PASSED");
    }

    /**
     * Test: Search with different query patterns simultaneously
     */
    @Test
    public void testConcurrentDifferentSearchPatterns() throws Exception {
        // Given
        int threadsPerPattern = 20;
        String[] searchPatterns = {"Company", "Sensor", "Location", "Zone", "Report"};
        ExecutorService executorService = Executors.newFixedThreadPool(
                threadsPerPattern * searchPatterns.length);
        CountDownLatch completionLatch = new CountDownLatch(
                threadsPerPattern * searchPatterns.length);

        AtomicInteger successCount = new AtomicInteger(0);
        Map<String, AtomicInteger> patternCounts = new ConcurrentHashMap<>();

        for (String pattern : searchPatterns) {
            patternCounts.put(pattern, new AtomicInteger(0));
        }

        System.out.println("Testing " + searchPatterns.length +
                " different search patterns with " + threadsPerPattern + " threads each...");

        // When - Execute different search patterns concurrently
        for (String pattern : searchPatterns) {
            for (int i = 0; i < threadsPerPattern; i++) {
                executorService.submit(() -> {
                    try {
                        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                                .query(pattern)
                                .page(0)
                                .size(20)
                                .build();

                        mockMvc.perform(post("/api/search")
                                        .header("Authorization", "Bearer " + authToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(searchRequest)))
                                .andExpect(status().isOk());

                        successCount.incrementAndGet();
                        patternCounts.get(pattern).incrementAndGet();

                    } catch (Exception e) {
                        System.err.println("Search failed for pattern " + pattern + ": " + e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
        }

        // Wait for completion
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertTrue(completed);

        System.out.println("\n=== Search Pattern Results ===");
        patternCounts.forEach((pattern, count) ->
                System.out.println(pattern + ": " + count.get() + " searches")
        );
        System.out.println("Total successful: " + successCount.get());

        assertThat(successCount.get()).isEqualTo(threadsPerPattern * searchPatterns.length);

        System.out.println("✓ Concurrent different patterns test PASSED");
    }

    /**
     * Test: Spike load - sudden burst of concurrent requests
     */
    @Test
    public void testSpikeLoad_SuddenBurstOfRequests() throws Exception {
        // Given
        int spikeUsers = 200; // Sudden spike to 200 users
        ExecutorService executorService = Executors.newFixedThreadPool(spikeUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(spikeUsers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        System.out.println("Testing spike load with sudden burst of " + spikeUsers + " users...");

        // When - All users hit at exactly the same time
        for (int i = 0; i < spikeUsers; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    long startTime = System.currentTimeMillis();

                    GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                            .query("Company")
                            .page(0)
                            .size(20)
                            .build();

                    mockMvc.perform(post("/api/search")
                                    .header("Authorization", "Bearer " + authToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(searchRequest)))
                            .andExpect(status().isOk());

                    long responseTime = System.currentTimeMillis() - startTime;
                    responseTimes.add(responseTime);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    System.err.println("Spike load request failed: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Release all threads at once
        startLatch.countDown();

        // Wait for completion
        boolean completed = completionLatch.await(90, TimeUnit.SECONDS);
        executorService.shutdown();

        // Then
        assertTrue(completed, "Spike load test should complete within 90 seconds");

        DoubleSummaryStatistics stats = responseTimes.stream()
                .mapToDouble(Long::doubleValue)
                .summaryStatistics();

        System.out.println("\n=== Spike Load Results ===");
        System.out.println("Successful requests: " + successCount.get() + "/" + spikeUsers);
        System.out.println("Average response time: " + String.format("%.2f", stats.getAverage()) + "ms");
        System.out.println("Max response time: " + (long) stats.getMax() + "ms");

        // Allow degraded performance during spike (up to 1000ms)
        assertThat(stats.getAverage()).isLessThan(1000);
        assertThat(successCount.get()).isGreaterThan((int)(spikeUsers * 0.95)); // 95% success rate

        System.out.println("✓ Spike load test PASSED");
    }
}
