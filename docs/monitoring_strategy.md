# Monitoring Strategy

## 1. Objectives
The monitoring strategy ensures the Global Search platform remains reliable, performant, and secure by tracking key metrics, configuring alerts, and enabling proactive response to issues before they impact users.

---

## 2. Metrics to Track

### 2.1 System Health
- **CPU usage** (per search node, admin services, gateway)
- **Memory usage** (heap/non-heap, garbage collection rates)
- **Disk usage** (index storage growth, log retention impact)

### 2.2 Query Performance
- **Query latency (p95, p99)** – average, slowest queries
- **Query throughput** – number of queries per second
- **Cache hit ratio** – to validate effectiveness of query caching

### 2.3 Reliability & Errors
- **Failed queries** – total failed searches per tenant
- **Timeouts / partial results** – count per timeframe
- **Indexing failures** – failed writes or mapping errors
- **Node health** – dropped nodes, cluster rebalancing events

### 2.4 Security & Access
- **Failed login attempts** (per user, per tenant)
- **Access violations** (unauthorized access attempts)
- **Policy decision logs** (allowed vs denied queries)

---

## 3. Alerts Configuration

### 3.1 Performance Thresholds
- **CPU > 80% sustained for 5 min** → Warning alert  
- **CPU > 90% sustained for 1 min** → Critical alert  
- **Query latency > 2s (p95)** → Warning alert  
- **Query latency > 5s (p99)** → Critical alert  

### 3.2 Reliability Thresholds
- **Failed queries > 5% of total per minute** → Warning  
- **Failed queries > 10% of total per minute** → Critical  
- **Node down > 30s** → Critical  

### 3.3 Security Thresholds
- **More than 5 failed logins per user in 10 min** → Warning  
- **Access violation attempts > 10 per tenant in 1 hr** → Critical  

---

## 4. Alert Channels
- **Email alerts** → Admin team distribution list  
- **Slack/MS Teams alerts** → #infra-alerts channel  
- **PagerDuty/On-call rotation** → Critical only  

---

## 5. Dashboards
Dashboards will be created in **Grafana/Kibana** to visualize:
- Real-time system metrics (CPU, memory, latency)
- Per-tenant query trends
- Security events (logins, access violations)
- Index/storage growth

---

## 6. Retention Policy
- **Logs**: Retain for 90 days, then archive to cold storage.  
- **Metrics**: Store at high resolution (1m intervals) for 30 days, downsample for long-term trends.  
- **Alerts**: Store alert history for 180 days for auditing purposes.  
