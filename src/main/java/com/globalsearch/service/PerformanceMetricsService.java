package com.globalsearch.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service to track and report performance metrics for search operations
 * Requirements: F3, F7.1-7.4 - Performance validation and monitoring
 */
@Service
@Slf4j
public class PerformanceMetricsService {

    // Store query execution times (last 1000 queries)
    private final List<QueryMetric> queryMetrics = new CopyOnWriteArrayList<>();
    private static final int MAX_METRICS_STORED = 1000;

    // Track metrics by tenant
    private final Map<String, TenantMetrics> tenantMetricsMap = new ConcurrentHashMap<>();

    /**
     * Record a query execution time
     */
    public void recordQueryExecution(String tenantId, String queryType, long executionTimeMs) {
        // Create metric
        QueryMetric metric = new QueryMetric(
            Instant.now(),
            tenantId,
            queryType,
            executionTimeMs
        );

        // Store in list (with size limit)
        queryMetrics.add(metric);
        if (queryMetrics.size() > MAX_METRICS_STORED) {
            queryMetrics.remove(0); // Remove oldest
        }

        // Update tenant metrics
        tenantMetricsMap.computeIfAbsent(tenantId, k -> new TenantMetrics(tenantId))
            .recordQuery(executionTimeMs);

        // Log slow queries (>1000ms violates requirement)
        if (executionTimeMs > 1000) {
            log.warn("SLOW QUERY DETECTED: Type={}, TenantId={}, ExecutionTime={}ms",
                queryType, tenantId, executionTimeMs);
        }
    }

    /**
     * Get overall performance statistics
     */
    public PerformanceStats getOverallStats() {
        if (queryMetrics.isEmpty()) {
            return new PerformanceStats();
        }

        List<Long> executionTimes = queryMetrics.stream()
            .map(QueryMetric::getExecutionTimeMs)
            .sorted()
            .toList();

        return calculateStats(executionTimes, queryMetrics.size());
    }

    /**
     * Get performance statistics for specific tenant
     */
    public PerformanceStats getTenantStats(String tenantId) {
        List<Long> executionTimes = queryMetrics.stream()
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(QueryMetric::getExecutionTimeMs)
            .sorted()
            .toList();

        if (executionTimes.isEmpty()) {
            return new PerformanceStats();
        }

        return calculateStats(executionTimes, executionTimes.size());
    }

    /**
     * Get recent slow queries (>1000ms)
     */
    public List<QueryMetric> getSlowQueries(int limit) {
        return queryMetrics.stream()
            .filter(m -> m.getExecutionTimeMs() > 1000)
            .sorted(Comparator.comparing(QueryMetric::getTimestamp).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * Get query distribution by latency buckets
     */
    public Map<String, Integer> getLatencyDistribution() {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("< 100ms", 0);
        distribution.put("100-500ms", 0);
        distribution.put("500-1000ms", 0);
        distribution.put("1000-2000ms", 0);
        distribution.put("> 2000ms", 0);

        for (QueryMetric metric : queryMetrics) {
            long time = metric.getExecutionTimeMs();
            if (time < 100) {
                distribution.merge("< 100ms", 1, Integer::sum);
            } else if (time < 500) {
                distribution.merge("100-500ms", 1, Integer::sum);
            } else if (time < 1000) {
                distribution.merge("500-1000ms", 1, Integer::sum);
            } else if (time < 2000) {
                distribution.merge("1000-2000ms", 1, Integer::sum);
            } else {
                distribution.merge("> 2000ms", 1, Integer::sum);
            }
        }

        return distribution;
    }

    /**
     * Check if performance meets SLA requirements
     */
    public SLAComplianceReport checkSLACompliance() {
        PerformanceStats stats = getOverallStats();

        boolean meetsAvgLatency = stats.getAverageDurationMs() < 500; // Should-have: < 500ms avg
        boolean meetsMaxLatency = stats.getP99DurationMs() < 1000;   // Must-have: < 1s under normal load

        long queriesOver1s = queryMetrics.stream()
            .filter(m -> m.getExecutionTimeMs() > 1000)
            .count();

        double violationRate = queryMetrics.isEmpty() ? 0 :
            (double) queriesOver1s / queryMetrics.size() * 100;

        return new SLAComplianceReport(
            meetsAvgLatency,
            meetsMaxLatency,
            stats.getAverageDurationMs(),
            stats.getP99DurationMs(),
            queriesOver1s,
            violationRate
        );
    }

    /**
     * Clear all metrics (for testing)
     */
    public void clearMetrics() {
        queryMetrics.clear();
        tenantMetricsMap.clear();
    }

    /**
     * Calculate statistics from execution times
     */
    private PerformanceStats calculateStats(List<Long> executionTimes, int totalQueries) {
        PerformanceStats stats = new PerformanceStats();
        stats.setTotalQueries(totalQueries);

        if (executionTimes.isEmpty()) {
            return stats;
        }

        // Average
        double avg = executionTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        stats.setAverageDurationMs((long) avg);

        // Min/Max
        stats.setMinDurationMs(executionTimes.get(0));
        stats.setMaxDurationMs(executionTimes.get(executionTimes.size() - 1));

        // Percentiles
        stats.setP50DurationMs(getPercentile(executionTimes, 50));
        stats.setP95DurationMs(getPercentile(executionTimes, 95));
        stats.setP99DurationMs(getPercentile(executionTimes, 99));

        return stats;
    }

    /**
     * Get percentile value from sorted list
     */
    private long getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    // Inner classes for metrics

    @Data
    public static class QueryMetric {
        private final Instant timestamp;
        private final String tenantId;
        private final String queryType;
        private final long executionTimeMs;
    }

    @Data
    public static class PerformanceStats {
        private int totalQueries;
        private long averageDurationMs;
        private long minDurationMs;
        private long maxDurationMs;
        private long p50DurationMs;
        private long p95DurationMs;
        private long p99DurationMs;
    }

    @Data
    public static class TenantMetrics {
        private final String tenantId;
        private int queryCount = 0;
        private long totalExecutionTime = 0;

        public void recordQuery(long executionTime) {
            queryCount++;
            totalExecutionTime += executionTime;
        }

        public double getAverageExecutionTime() {
            return queryCount == 0 ? 0 : (double) totalExecutionTime / queryCount;
        }
    }

    @Data
    public static class SLAComplianceReport {
        private final boolean meetsAverageLatencySLA;
        private final boolean meetsMaxLatencySLA;
        private final long actualAverageLatencyMs;
        private final long actualP99LatencyMs;
        private final long queriesExceeding1Second;
        private final double violationPercentage;

        public boolean isCompliant() {
            return meetsAverageLatencySLA && meetsMaxLatencySLA;
        }
    }
}
