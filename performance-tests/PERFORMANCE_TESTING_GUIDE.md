# Performance Testing Guide
## Global Search System - 7th Semester Project

This guide covers performance validation for requirements F3, F7.1-7.4, and U14.

---

## Requirements Being Validated

### Must-Have Requirements:
- **F3**: System must return results with < 1 second latency under normal load
- **F3.1**: Average response time for queries < 500 ms (Should-have)
- **F7.1**: Must support growth of at least 30GB/month without downtime
- **F7.2**: Must scale beyond 2TB total database size without redesign
- **F7.3**: The system must maintain indices for queries across large datasets
- **F7.4**: Query performance must degrade <20% even at 5TB scale
- **U14**: Indexing latency below 5 minutes

---

## 1. Indexing Latency Test (U14)

### Test Class
`src/test/java/com/globalsearch/performance/IndexingLatencyTest.java`

### What It Tests
- Time from MySQL entity creation to Elasticsearch searchability
- Tests Company, Location, and Sensor indexing
- Bulk indexing performance (10 entities)

### How to Run

```bash
cd C:\Users\Zubeyr\IdeaProjects\Global-Search-

# Run all indexing latency tests
mvn test -Dtest=IndexingLatencyTest

# Run specific test
mvn test -Dtest=IndexingLatencyTest#testCompanyIndexingLatency
```

### Expected Results
- ✅ All entities indexed within 5 minutes (300 seconds)
- ✅ Typical indexing: 2-10 seconds for real-time sync
- ✅ Bulk operations: < 30 seconds for 10 entities

### Pass Criteria
```
Company indexing latency: < 300 seconds ✓
Location indexing latency: < 300 seconds ✓
Sensor indexing latency: < 300 seconds ✓
Bulk indexing (10 entities): < 300 seconds ✓
```

---

## 2. Load Testing with JMeter (F3, F3.1, F7.4)

### Prerequisites

#### Install Apache JMeter
1. Download from: https://jmeter.apache.org/download_jmeter.cgi
2. Extract to `C:\Tools\apache-jmeter-5.6.2`
3. Verify installation:
   ```bash
   C:\Tools\apache-jmeter-5.6.2\bin\jmeter.bat
   ```

### Test Plans

#### Test Plan 1: Normal Load (100 Users)
**File**: `performance-tests/jmeter/Global-Search-Load-Test.jmx`

**Configuration**:
- Threads: 100 concurrent users
- Ramp-up: 10 seconds
- Loop Count: 10 requests per user
- Total Requests: 1000

**Endpoint Tested**:
```
POST /api/search
{
  "query": "sensor",
  "page": 0,
  "size": 20
}
```

**Assertions**:
- Response time < 1000ms (Must-have F3)
- Success rate > 99%

### How to Run JMeter Tests

#### Step 1: Get JWT Token
```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin",
    "password": "admin123"
  }'

# Copy the "token" value from response
```

#### Step 2: Update JMeter Test Plan
1. Open `Global-Search-Load-Test.jmx` in JMeter GUI
2. Find "User Defined Variables"
3. Update `JWT_TOKEN` with actual token from Step 1
4. Save the test plan

#### Step 3: Run Test

**GUI Mode (for development)**:
```bash
C:\Tools\apache-jmeter-5.6.2\bin\jmeter.bat -t "C:\Users\Zubeyr\IdeaProjects\Global-Search-\performance-tests\jmeter\Global-Search-Load-Test.jmx"
```

**Command Line Mode (for production testing)**:
```bash
C:\Tools\apache-jmeter-5.6.2\bin\jmeter.bat ^
  -n ^
  -t "C:\Users\Zubeyr\IdeaProjects\Global-Search-\performance-tests\jmeter\Global-Search-Load-Test.jmx" ^
  -l "C:\Users\Zubeyr\IdeaProjects\Global-Search-\performance-tests\results\results.jtl" ^
  -e ^
  -o "C:\Users\Zubeyr\IdeaProjects\Global-Search-\performance-tests\results\html-report"
```

### Analyzing Results

#### View Results in JMeter GUI
1. Summary Report: Shows avg, min, max, throughput
2. Aggregate Report: Shows percentiles (90th, 95th, 99th)
3. View Results Tree: Shows individual request details

#### HTML Report
Open: `performance-tests/results/html-report/index.html`

**Key Metrics to Check**:
- Average Response Time: Should be < 500ms ✓ (F3.1)
- 90th Percentile: Should be < 800ms ✓
- 99th Percentile: Should be < 1000ms ✓ (F3)
- Error Rate: Should be < 1% ✓
- Throughput: Requests per second

---

## 3. Performance Monitoring API (F3, U16)

### Endpoints

#### Get Overall Performance Stats
```bash
curl -X GET http://localhost:8080/api/performance/stats \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "totalQueries": 1000,
  "averageDurationMs": 245,
  "minDurationMs": 12,
  "maxDurationMs": 987,
  "p50DurationMs": 234,
  "p95DurationMs": 456,
  "p99DurationMs": 789
}
```

#### Check SLA Compliance
```bash
curl -X GET http://localhost:8080/api/performance/sla-compliance \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "meetsAverageLatencySLA": true,
  "meetsMaxLatencySLA": true,
  "actualAverageLatencyMs": 245,
  "actualP99LatencyMs": 789,
  "queriesExceeding1Second": 3,
  "violationPercentage": 0.3,
  "compliant": true
}
```

#### Get Slow Queries
```bash
curl -X GET "http://localhost:8080/api/performance/slow-queries?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Get Latency Distribution
```bash
curl -X GET http://localhost:8080/api/performance/distribution \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "< 100ms": 234,
  "100-500ms": 654,
  "500-1000ms": 98,
  "1000-2000ms": 12,
  "> 2000ms": 2
}
```

---

## 4. Test Scenarios

### Scenario 1: Normal Load Test
**Objective**: Validate F3 and F3.1 under normal conditions

**Configuration**:
- 100 concurrent users
- 10 requests each
- Simple search queries

**Pass Criteria**:
- ✅ Average latency < 500ms
- ✅ 99th percentile < 1000ms
- ✅ Error rate < 1%

**Command**:
```bash
mvn clean install
mvn spring-boot:run -Dmaven.test.skip=true

# In another terminal:
# Run JMeter test as described above
```

---

### Scenario 2: Advanced Search Load Test
**Objective**: Test complex queries with filters

**Test Query**:
```json
{
  "query": "temperature sensor",
  "entityTypes": ["Sensor"],
  "filters": {
    "city": "Copenhagen",
    "sensorType": "TEMPERATURE"
  },
  "enableFuzzySearch": true,
  "enableHighlighting": true
}
```

**Configuration**:
- 50 concurrent users
- 20 requests each
- Advanced search with multiple filters

**Pass Criteria**:
- ✅ Average latency < 800ms
- ✅ 99th percentile < 1500ms

---

### Scenario 3: Stress Test
**Objective**: Validate F7.4 (< 20% degradation under stress)

**Configuration**:
- 200 concurrent users
- 10 requests each
- Ramp-up: 20 seconds

**Steps**:
1. Run baseline test (100 users) - Record average latency
2. Run stress test (200 users) - Record average latency
3. Calculate degradation: `(StressLatency - BaselineLatency) / BaselineLatency * 100`

**Pass Criteria**:
- ✅ Degradation < 20%
- ✅ Error rate < 5%
- ✅ No server crashes

**Example Calculation**:
```
Baseline (100 users): 400ms average
Stress (200 users): 460ms average
Degradation: (460 - 400) / 400 * 100 = 15% ✓ (< 20%)
```

---

### Scenario 4: Database Growth Simulation (F7.1, F7.2)
**Objective**: Validate system handles 30GB/month growth

**Current State**:
- Database: ~1TB
- Growth rate: 15-30GB/month
- Largest table: Sensor data (~800GB)

**Test Approach** (Manual):
1. Check current query performance
2. Add test data (simulate 30GB growth)
3. Recheck query performance
4. Validate degradation < 5%

**Query to Monitor**:
```sql
-- Check query performance before and after data growth
EXPLAIN SELECT * FROM sensors WHERE tenant_id = 1 AND sensor_type = 'TEMPERATURE';
```

**Pass Criteria**:
- ✅ Indices maintained correctly
- ✅ Query execution time increase < 5%
- ✅ No table scan queries

---

## 5. Performance Benchmarks

### Expected Performance Metrics

| Metric | Target | Measured | Status |
|--------|--------|----------|--------|
| Average Latency | < 500ms | TBD | ⏳ |
| 95th Percentile | < 800ms | TBD | ⏳ |
| 99th Percentile | < 1000ms | TBD | ⏳ |
| Indexing Latency | < 5 min | TBD | ⏳ |
| Error Rate | < 1% | TBD | ⏳ |
| Throughput | > 50 req/s | TBD | ⏳ |
| Degradation @ 2x load | < 20% | TBD | ⏳ |

---

## 6. Troubleshooting

### Slow Queries Detected

**Check**:
```bash
# View slow queries via API
curl -X GET "http://localhost:8080/api/performance/slow-queries?limit=50" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Solutions**:
1. Check Elasticsearch indices health:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```
2. Optimize indices:
   ```bash
   curl -X POST "http://localhost:9200/sensors/_forcemerge?max_num_segments=1"
   ```
3. Check MySQL slow query log
4. Add database indices if needed

---

### High Error Rate

**Check Application Logs**:
```bash
# Look for errors
tail -f logs/spring-boot-application.log | grep ERROR
```

**Common Issues**:
- JWT token expired (get new token)
- Elasticsearch connection timeout
- MySQL connection pool exhausted
- Out of memory errors

---

## 7. Continuous Monitoring

### Production Monitoring

**Endpoints to Monitor**:
- `/api/performance/stats` - Every 5 minutes
- `/api/performance/sla-compliance` - Every 15 minutes
- `/api/performance/slow-queries` - When violations detected

**Alert Thresholds**:
- Average latency > 500ms for 5 minutes → WARNING
- Average latency > 1000ms for 5 minutes → CRITICAL
- Error rate > 5% → CRITICAL
- Slow queries > 50 in 1 hour → WARNING

---

## 8. Test Results Template

### Test Execution Report

```
=================================================================
PERFORMANCE TEST RESULTS - Global Search System
=================================================================
Date: 2025-01-XX
Tester: [Your Name]
Environment: Development / Staging / Production

-----------------------------------------------------------------
1. INDEXING LATENCY TEST (U14)
-----------------------------------------------------------------
Company Indexing: XX seconds ✓/✗
Location Indexing: XX seconds ✓/✗
Sensor Indexing: XX seconds ✓/✗
Bulk Indexing (10): XX seconds ✓/✗

Pass Criteria: < 300 seconds
Status: PASS / FAIL

-----------------------------------------------------------------
2. NORMAL LOAD TEST (F3, F3.1)
-----------------------------------------------------------------
Concurrent Users: 100
Total Requests: 1000
Duration: XX seconds

Average Latency: XXX ms ✓/✗ (target: < 500ms)
Min Latency: XX ms
Max Latency: XXX ms
50th Percentile: XXX ms
95th Percentile: XXX ms ✓/✗ (target: < 800ms)
99th Percentile: XXX ms ✓/✗ (target: < 1000ms)
Error Rate: X.X% ✓/✗ (target: < 1%)
Throughput: XX req/s

Status: PASS / FAIL

-----------------------------------------------------------------
3. STRESS TEST (F7.4)
-----------------------------------------------------------------
Baseline Latency (100 users): XXX ms
Stress Latency (200 users): XXX ms
Degradation: XX% ✓/✗ (target: < 20%)

Status: PASS / FAIL

-----------------------------------------------------------------
OVERALL RESULT: PASS / FAIL
-----------------------------------------------------------------
Notes:
- [Add any observations]
- [Add any issues encountered]
- [Add recommendations]
=================================================================
```

---

## 9. Next Steps

1. ✅ Run indexing latency tests
2. ✅ Run normal load test (100 users)
3. ✅ Run stress test (200 users)
4. ✅ Document results using template above
5. ✅ Fix any performance issues found
6. ✅ Re-run tests to verify fixes
7. ✅ Archive results for project documentation

---

## References

- JMeter Documentation: https://jmeter.apache.org/usermanual/
- Spring Boot Performance: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- Elasticsearch Performance: https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-search-speed.html
