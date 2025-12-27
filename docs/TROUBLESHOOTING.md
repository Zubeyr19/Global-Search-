# Global Search - Troubleshooting Guide

## Table of Contents
1. [Common Issues](#common-issues)
2. [Application Startup Problems](#application-startup-problems)
3. [Database Issues](#database-issues)
4. [Elasticsearch Issues](#elasticsearch-issues)
5. [Authentication & Authorization](#authentication--authorization)
6. [Search Problems](#search-problems)
7. [Performance Issues](#performance-issues)
8. [Integration Problems](#integration-problems)
9. [Logging and Monitoring](#logging-and-monitoring)
10. [Production Issues](#production-issues)

---

## Common Issues

### Application Won't Start

**Problem:** Application fails to start with error messages

**Solutions:**

1. **Port Already in Use (Port 8080)**
   ```bash
   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F

   # Linux/Mac
   lsof -i :8080
   kill -9 <PID>
   ```

   Alternative: Change port in `application.properties`:
   ```properties
   server.port=8081
   ```

2. **Java Version Mismatch**
   ```bash
   # Check Java version
   java -version

   # Should be Java 17 or higher
   # Update JAVA_HOME environment variable if needed
   ```

3. **Maven Dependencies Not Downloaded**
   ```bash
   # Force update
   mvn clean install -U

   # Clear local repository cache
   rm -rf ~/.m2/repository/com/globalsearch
   mvn clean install
   ```

4. **Lombok Not Working**
   - IntelliJ: Enable annotation processing
     - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
     - Check "Enable annotation processing"
   - Install Lombok plugin for your IDE
   - Rebuild project

---

## Application Startup Problems

### Database Connection Failures

**Error:** `java.sql.SQLExceptionConnection refused` or `Unknown database 'global_search_db'`

**Solutions:**

1. **Verify MySQL is Running**
   ```bash
   # Windows
   net start MySQL80

   # Linux
   sudo systemctl status mysql
   sudo systemctl start mysql

   # Mac
   brew services start mysql
   ```

2. **Check Database Exists**
   ```sql
   mysql -u root -p
   SHOW DATABASES;

   # If not exists, create it
   CREATE DATABASE global_search_db;
   ```

3. **Verify Credentials**
   Check `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/global_search_db
   spring.datasource.username=root
   spring.datasource.password=YourPassword
   ```

4. **Test Connection Manually**
   ```bash
   mysql -u root -p -h localhost global_search_db
   ```

5. **Check MySQL User Permissions**
   ```sql
   GRANT ALL PRIVILEGES ON global_search_db.* TO 'root'@'localhost';
   FLUSH PRIVILEGES;
   ```

### Elasticsearch Connection Failures

**Error:** `NoNodeAvailableException` or `Connection refused to localhost:9200`

**Solutions:**

1. **Verify Elasticsearch is Running**
   ```bash
   # Test connection
   curl http://localhost:9200

   # Expected response: JSON with cluster info
   ```

2. **Start Elasticsearch**
   ```bash
   # Navigate to ES directory
   cd /path/to/elasticsearch-8.13.2

   # Start
   bin/elasticsearch         # Linux/Mac
   bin\elasticsearch.bat     # Windows
   ```

3. **Check Elasticsearch Logs**
   ```bash
   tail -f /path/to/elasticsearch/logs/elasticsearch.log
   ```

4. **Verify Configuration**
   `application.properties`:
   ```properties
   spring.elasticsearch.uris=http://localhost:9200
   spring.elasticsearch.connection-timeout=5s
   spring.elasticsearch.socket-timeout=30s
   ```

5. **Check Firewall**
   Ensure port 9200 is not blocked by firewall

---

## Database Issues

### Tables Not Created

**Problem:** Application starts but tables are missing

**Solutions:**

1. **Check Hibernate DDL Setting**
   `application.properties`:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```

2. **Enable SQL Logging**
   ```properties
   logging.level.org.hibernate.SQL=DEBUG
   logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
   ```

3. **Manually Create Tables**
   If DDL auto fails, run SQL scripts manually:
   ```bash
   mysql -u root -p global_search_db < schema.sql
   ```

### Data Not Persisting

**Problem:** Data saves but doesn't appear in database

**Solutions:**

1. **Check Transaction Management**
   ```java
   @Transactional
   public void saveData(Entity entity) {
       repository.save(entity);
   }
   ```

2. **Verify Auto-commit**
   ```properties
   spring.datasource.hikari.auto-commit=true
   ```

3. **Check for Exceptions**
   Enable debug logging to see rollback reasons

### Slow Database Queries

**Problem:** Database queries taking too long

**Solutions:**

1. **Add Indexes**
   ```sql
   CREATE INDEX idx_tenant_id ON users(tenant_id);
   CREATE INDEX idx_sensor_type ON sensors(sensor_type, tenant_id);
   ```

2. **Analyze Slow Queries**
   ```sql
   -- Enable slow query log
   SET GLOBAL slow_query_log = 'ON';
   SET GLOBAL long_query_time = 1;

   -- Check slow queries
   SELECT * FROM mysql.slow_log;
   ```

3. **Optimize HikariCP**
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   spring.datasource.hikari.minimum-idle=10
   spring.datasource.hikari.connection-timeout=30000
   ```

4. **Use Query Optimization**
   ```java
   // Bad
   List<Company> companies = repository.findAll();

   // Good - with pagination
   Page<Company> companies = repository.findAll(PageRequest.of(0, 20));
   ```

---

## Elasticsearch Issues

### Index Not Created

**Problem:** Elasticsearch indices are not being created

**Solutions:**

1. **Manual Index Creation**
   ```bash
   curl -X PUT "localhost:9200/companies" -H 'Content-Type: application/json' -d'
   {
     "mappings": {
       "properties": {
         "name": { "type": "text" },
         "tenantId": { "type": "keyword" }
       }
     }
   }'
   ```

2. **Trigger Initial Sync**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/elasticsearch/sync/all" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **Check Elasticsearch Logs**
   ```bash
   # Look for errors in ES logs
   tail -f /path/to/elasticsearch/logs/elasticsearch.log
   ```

### Search Returns No Results

**Problem:** Search queries return empty results

**Solutions:**

1. **Verify Data is Indexed**
   ```bash
   # Check document count
   curl "localhost:9200/companies/_count"

   # Search all documents
   curl "localhost:9200/companies/_search?pretty"
   ```

2. **Check Tenant Filtering**
   Ensure tenant_id is correctly set in search queries

3. **Verify Sync Status**
   Check application logs for sync errors:
   ```bash
   grep "Elasticsearch sync" application.log
   ```

4. **Reindex All Data**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/elasticsearch/sync/all" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

### Index Mapping Conflicts

**Problem:** Field type conflicts or mapping errors

**Solutions:**

1. **Delete and Recreate Index**
   ```bash
   # Delete index
   curl -X DELETE "localhost:9200/companies"

   # Restart application to recreate
   ```

2. **Update Mapping**
   ```bash
   curl -X PUT "localhost:9200/companies/_mapping" -H 'Content-Type: application/json' -d'
   {
     "properties": {
       "newField": { "type": "keyword" }
     }
   }'
   ```

---

## Authentication & Authorization

### JWT Token Invalid

**Problem:** "Invalid token" or "Token expired" errors

**Solutions:**

1. **Check Token Expiration**
   JWT tokens expire after 24 hours by default
   ```bash
   # Get new token
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"user","password":"pass"}'
   ```

2. **Verify JWT Secret**
   Ensure same secret is used across restarts:
   ```properties
   jwt.secret=YourSecretKeyHere
   jwt.expiration=86400000
   ```

3. **Check Token Format**
   Token should be sent as: `Authorization: Bearer {token}`

4. **Decode Token for Debugging**
   Use https://jwt.io to decode and verify token contents

### Account Locked

**Problem:** "Account is temporarily locked" message

**Solutions:**

1. **Wait for Lockout Period**
   Default: 30 minutes

2. **Admin Unlock**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/users/{userId}/unlock" \
     -H "Authorization: Bearer ADMIN_TOKEN"
   ```

3. **Check Failed Attempts**
   ```java
   // Service logs show failed attempt count
   grep "Login failed for user" application.log
   ```

### 403 Forbidden Errors

**Problem:** User cannot access endpoint despite being authenticated

**Solutions:**

1. **Check User Roles**
   ```bash
   curl "http://localhost:8080/api/auth/me" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

2. **Verify Role Requirements**
   Check `SecurityConfig.java` for endpoint role mappings:
   ```java
   .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
   ```

3. **Update User Role**
   ```sql
   INSERT INTO user_roles (user_id, role) VALUES (1, 'MANAGER');
   ```

---

## Search Problems

### Empty Search Results

**Problem:** Searches return no results despite data existing

**Solutions:**

1. **Check Tenant Isolation**
   User can only see data from their tenant
   ```sql
   SELECT * FROM companies WHERE tenant_id = 'USER_TENANT';
   ```

2. **Verify Elasticsearch Sync**
   ```bash
   # Check if data is in ES
   curl "localhost:9200/companies/_search?q=*&pretty"
   ```

3. **Test with Admin Account**
   Admin can search across all tenants

4. **Check Search Query**
   Enable debug logging:
   ```properties
   logging.level.com.globalsearch.service.search=DEBUG
   ```

### Fuzzy Search Not Working

**Problem:** Fuzzy search doesn't find misspelled terms

**Solutions:**

1. **Enable Fuzzy Search in Request**
   ```json
   {
     "query": "sensr",
     "enableFuzzySearch": true,
     "fuzzyMaxEdits": 2
   }
   ```

2. **Check Edit Distance**
   Default maximum edits: 2
   Increase if needed, but impacts performance

3. **Verify Field Indexing**
   Ensure fields are indexed as `text` type in Elasticsearch

### Slow Search Performance

**Problem:** Search queries taking >1 second

**Solutions:**

1. **Check Result Size**
   Limit results per page:
   ```json
   {
     "query": "sensor",
     "size": 20
   }
   ```

2. **Use Entity Type Filters**
   ```json
   {
     "query": "sensor",
     "entityTypes": ["Sensor"]
   }
   ```

3. **Disable Highlighting**
   Highlighting adds overhead:
   ```json
   {
     "query": "sensor",
     "enableHighlighting": false
   }
   ```

4. **Check Cache**
   Verify caching is enabled:
   ```properties
   spring.cache.type=caffeine
   ```

5. **Monitor Elasticsearch**
   ```bash
   curl "localhost:9200/_cat/indices?v"
   curl "localhost:9200/_cluster/health?pretty"
   ```

---

## Performance Issues

### High Memory Usage

**Problem:** Application using too much memory

**Solutions:**

1. **Increase JVM Heap**
   ```bash
   java -Xms512m -Xmx2048m -jar application.jar
   ```

2. **Configure in Maven**
   ```bash
   MAVEN_OPTS="-Xmx1024m" mvn spring-boot:run
   ```

3. **Monitor Memory**
   ```bash
   jstat -gc <pid> 1000
   ```

4. **Check for Memory Leaks**
   Use VisualVM or JProfiler

5. **Optimize Cache Sizes**
   ```properties
   spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=10m
   ```

### Slow API Responses

**Problem:** API endpoints responding slowly

**Solutions:**

1. **Enable Response Compression**
   Already enabled:
   ```properties
   server.compression.enabled=true
   ```

2. **Check Database Connection Pool**
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   ```

3. **Add Indexes**
   See database slow query analysis above

4. **Use Async Processing**
   ```java
   @Async
   public CompletableFuture<Result> processAsync() {
       // Long operation
   }
   ```

5. **Monitor with Actuator**
   ```bash
   curl "http://localhost:8080/actuator/metrics/http.server.requests"
   ```

### High CPU Usage

**Problem:** CPU usage at 100%

**Solutions:**

1. **Check Thread Pool**
   ```properties
   server.tomcat.threads.max=200
   server.tomcat.threads.min-spare=20
   ```

2. **Profile Application**
   ```bash
   # Use JProfiler or YourKit
   jstack <pid> > thread_dump.txt
   ```

3. **Check for Infinite Loops**
   Review recent code changes

4. **Optimize Elasticsearch Queries**
   Use filters instead of queries when possible

---

## Integration Problems

### WebSocket Connection Fails

**Problem:** Cannot establish WebSocket connection

**Solutions:**

1. **Check WebSocket URL**
   ```javascript
   const socket = new SockJS('http://localhost:8080/ws');
   ```

2. **Verify CORS Configuration**
   ```java
   registry.addEndpoint("/ws")
           .setAllowedOrigins("http://localhost:3000")
           .withSockJS();
   ```

3. **Check Firewall/Proxy**
   WebSockets may be blocked by corporate firewalls

### Rate Limiting Too Restrictive

**Problem:** Getting 429 Too Many Requests errors

**Solutions:**

1. **Check Current Limits**
   Default: 100 requests per minute

2. **Increase Limits (Dev Only)**
   Modify `RateLimitInterceptor.java`

3. **Implement Exponential Backoff**
   ```javascript
   async function retryWithBackoff(fn, retries = 3) {
     for (let i = 0; i < retries; i++) {
       try {
         return await fn();
       } catch (error) {
         if (i === retries - 1) throw error;
         await new Promise(r => setTimeout(r, 2 ** i * 1000));
       }
     }
   }
   ```

---

## Logging and Monitoring

### Enable Debug Logging

**Temporary (Runtime):**
```bash
# Via API (if actuator enabled)
curl -X POST "http://localhost:8080/actuator/loggers/com.globalsearch" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

**Permanent:**
`application.properties`:
```properties
logging.level.com.globalsearch=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Log File Location

```properties
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30
```

### Common Log Patterns

**Authentication Issues:**
```bash
grep "authentication" logs/application.log
```

**Search Errors:**
```bash
grep "search" logs/application.log | grep ERROR
```

**Database Errors:**
```bash
grep "SQL" logs/application.log | grep ERROR
```

---

## Production Issues

### Application Crashes

**Problem:** Application stops unexpectedly

**Solutions:**

1. **Check System Resources**
   ```bash
   free -h          # Memory
   df -h            # Disk space
   top              # CPU usage
   ```

2. **Review Logs**
   ```bash
   tail -n 500 logs/application.log
   journalctl -u global-search -n 200
   ```

3. **Check JVM Crash Logs**
   ```bash
   ls -la hs_err_pid*.log
   cat hs_err_pid12345.log
   ```

4. **Monitor with Systemd**
   ```bash
   # Auto-restart on failure
   sudo systemctl status global-search
   sudo systemctl restart global-search
   ```

### Data Loss or Corruption

**Problem:** Data missing or corrupted

**Solutions:**

1. **Check Database Backups**
   ```bash
   # Restore from backup
   mysql -u root -p global_search_db < backup.sql
   ```

2. **Verify Transaction Logs**
   ```bash
   mysqlbinlog mysql-bin.000001 > transactions.sql
   ```

3. **Resync Elasticsearch**
   ```bash
   curl -X POST "http://localhost:8080/api/admin/elasticsearch/sync/all"
   ```

### Security Breaches

**Problem:** Unauthorized access detected

**Solutions:**

1. **Change JWT Secret Immediately**
   ```properties
   jwt.secret=NEW_SECRET_KEY_HERE
   ```

2. **Invalidate All Sessions**
   Restart application

3. **Review Audit Logs**
   ```sql
   SELECT * FROM audit_logs
   WHERE action = 'LOGIN_FAILED'
   AND timestamp > NOW() - INTERVAL 1 DAY
   ORDER BY timestamp DESC;
   ```

4. **Check for SQL Injection**
   Review recent search queries in audit logs

5. **Update Passwords**
   Force password reset for all users

---

## Getting Help

### Before Requesting Support

1. Check this troubleshooting guide
2. Review application logs
3. Search GitHub issues
4. Try with a minimal reproduction

### Information to Provide

When reporting issues, include:
- Application version / Git commit hash
- Java version
- MySQL version
- Elasticsearch version
- Full error stack trace
- Steps to reproduce
- Expected vs actual behavior
- Relevant log excerpts

### Contact

- **Issues:** https://github.com/Zubeyr19/Global-Search-/issues
- **Email:** zuabd22@student.sdu.dk
- **Documentation:** `/docs` folder

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Maintained By:** Development Team
