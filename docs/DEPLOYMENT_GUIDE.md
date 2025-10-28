# Global Search - Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Database Configuration](#database-configuration)
4. [Elasticsearch Setup](#elasticsearch-setup)
5. [Application Configuration](#application-configuration)
6. [Building the Application](#building-the-application)
7. [Running the Application](#running-the-application)
8. [Verification](#verification)
9. [Troubleshooting](#troubleshooting)
10. [Production Deployment](#production-deployment)

---

## Prerequisites

### Required Software
- **Java:** JDK 17 or higher
- **Maven:** 3.9+
- **MySQL:** 8.0+
- **Elasticsearch:** 8.13.2+
- **Git:** For version control

### System Requirements
- **Memory:** Minimum 8GB RAM (16GB recommended)
- **Storage:** 50GB+ available space
- **OS:** Windows 10/11, Linux, or macOS

### Download Links
- Java 17: https://adoptium.net/
- Maven: https://maven.apache.org/download.cgi
- MySQL: https://dev.mysql.com/downloads/
- Elasticsearch: https://www.elastic.co/downloads/elasticsearch

---

## Local Development Setup

### Step 1: Install MySQL 8.0

#### Windows
1. Download MySQL 8.0 installer from https://dev.mysql.com/downloads/installer/
2. Run the installer and choose "Developer Default"
3. Set root password (remember this!)
4. Complete installation

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo mysql_secure_installation
```

#### macOS
```bash
brew install mysql
brew services start mysql
mysql_secure_installation
```

### Step 2: Create Database

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE global_search_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application user (optional but recommended)
CREATE USER 'globalsearch'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON global_search_db.* TO 'globalsearch'@'localhost';
FLUSH PRIVILEGES;

-- Verify database
SHOW DATABASES;
USE global_search_db;
```

---

## Elasticsearch Setup

### Step 1: Download and Extract

#### Windows
1. Download Elasticsearch 8.13.2 from https://www.elastic.co/downloads/elasticsearch
2. Extract to: `C:\elasticsearch-8.13.2`

#### Linux/macOS
```bash
cd ~
wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.13.2-linux-x86_64.tar.gz
tar -xzf elasticsearch-8.13.2-linux-x86_64.tar.gz
cd elasticsearch-8.13.2
```

### Step 2: Configure Elasticsearch

Edit `config/elasticsearch.yml`:

```yaml
# Cluster name
cluster.name: global-search-cluster

# Node name
node.name: node-1

# Network settings
network.host: localhost
http.port: 9200

# Disable security for development (ENABLE IN PRODUCTION!)
xpack.security.enabled: false
xpack.security.enrollment.enabled: false
xpack.security.http.ssl.enabled: false
xpack.security.transport.ssl.enabled: false
```

### Step 3: Start Elasticsearch

#### Windows
```cmd
cd C:\elasticsearch-8.13.2
bin\elasticsearch.bat
```

#### Linux/macOS
```bash
cd ~/elasticsearch-8.13.2
./bin/elasticsearch
```

### Step 4: Verify Elasticsearch

```bash
# Check if running
curl http://localhost:9200

# Expected output:
{
  "name" : "node-1",
  "cluster_name" : "global-search-cluster",
  "version" : {
    "number" : "8.13.2",
    ...
  }
}
```

---

## Application Configuration

### Step 1: Clone Repository

```bash
git clone https://github.com/Zubeyr19/Global-Search-.git
cd Global-Search-
```

### Step 2: Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080
spring.application.name=global-search

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/global_search_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Elasticsearch Configuration
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.connection-timeout=5s
spring.elasticsearch.socket-timeout=30s
spring.data.elasticsearch.repositories.enabled=true

# Logging
logging.level.root=INFO
logging.level.com.globalsearch=INFO

# JWT Configuration
jwt.secret=YOUR_SECRET_KEY_HERE_MAKE_IT_LONG_AND_RANDOM
jwt.expiration=86400000
jwt.refresh.expiration=604800000
```

**⚠️ IMPORTANT:**
- Replace `YOUR_MYSQL_PASSWORD` with your actual MySQL password
- Generate a strong JWT secret (at least 32 characters)
- Never commit real credentials to version control!

---

## Building the Application

### Clean and Compile

```bash
# Navigate to project root
cd C:\Users\Zubeyr\IdeaProjects\Global-Search-

# Clean previous builds
mvn clean

# Compile
mvn compile

# Run tests (optional)
mvn test

# Package (creates JAR file)
mvn clean package -DskipTests
```

### Build Output

After successful build:
```
target/
├── global-search-1.0-SNAPSHOT.jar  ← Main JAR file
├── classes/
└── test-classes/
```

---

## Running the Application

### Option 1: Using Maven (Development)

```bash
# Start application
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests
mvn spring-boot:run -Dmaven.test.skip=true
```

### Option 2: Using JAR File (Production)

```bash
# Build JAR first
mvn clean package -DskipTests

# Run JAR
java -jar target/global-search-1.0-SNAPSHOT.jar

# With custom memory settings
java -Xmx2g -Xms1g -jar target/global-search-1.0-SNAPSHOT.jar

# With specific profile
java -jar target/global-search-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### Option 3: IDE (IntelliJ IDEA / Eclipse)

1. Import project as Maven project
2. Wait for dependencies to download
3. Run `GlobalSearchApplication.java` main method

---

## Verification

### Step 1: Check Application Startup

Look for this in console output:
```
2025-10-28T21:15:56.347  INFO --- Started GlobalSearchApplication in 4.835 seconds
=================================
Global Search Application Started!
Visit: http://localhost:8080
=================================
```

### Step 2: Verify Database Connection

```sql
-- Connect to MySQL
mysql -u root -p

-- Check if tables were created
USE global_search_db;
SHOW TABLES;

-- Expected tables:
companies, locations, zones, sensors, reports, dashboards,
users, audit_logs, policies
```

### Step 3: Verify Elasticsearch Indices

```bash
# Check indices
curl http://localhost:9200/_cat/indices?v

# Expected output:
health status index      docs.count
yellow open   companies  2
yellow open   locations  2
yellow open   zones      2
yellow open   sensors    4
yellow open   reports    0
yellow open   dashboards 0
```

### Step 4: Test API Endpoints

#### Health Check
```bash
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

#### Swagger UI
Open browser: http://localhost:8080/swagger-ui.html

#### Test Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}'

# Expected: JWT token response
```

---

## Troubleshooting

### Issue 1: Port 8080 Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**
```bash
# Windows - Find and kill process
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/macOS
lsof -i :8080
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

### Issue 2: MySQL Connection Refused

**Error:** `Communications link failure`

**Solutions:**
1. Check MySQL is running:
   ```bash
   # Windows
   services.msc  # Look for MySQL80

   # Linux
   sudo systemctl status mysql

   # macOS
   brew services list
   ```

2. Verify credentials in `application.properties`
3. Check firewall settings
4. Try `localhost` vs `127.0.0.1`

### Issue 3: Elasticsearch Not Responding

**Error:** `Connection refused to http://localhost:9200`

**Solutions:**
1. Check if Elasticsearch is running:
   ```bash
   curl http://localhost:9200
   ```

2. Check logs:
   ```bash
   # Windows
   type C:\elasticsearch-8.13.2\logs\global-search-cluster.log

   # Linux/macOS
   tail -f ~/elasticsearch-8.13.2/logs/global-search-cluster.log
   ```

3. Increase heap size (if out of memory):
   ```bash
   # Edit config/jvm.options
   -Xms1g
   -Xmx2g
   ```

### Issue 4: OutOfMemoryError

**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:**
```bash
# Increase JVM memory
java -Xmx4g -Xms2g -jar target/global-search-1.0-SNAPSHOT.jar

# Or set in MAVEN_OPTS
export MAVEN_OPTS="-Xmx4g -Xms2g"
mvn spring-boot:run
```

### Issue 5: Hibernate DDL Issues

**Error:** `Table 'companies' doesn't exist`

**Solution:**
1. Check `spring.jpa.hibernate.ddl-auto=update` in properties
2. Manually create tables using schema SQL
3. Or use `ddl-auto=create` (⚠️ WARNING: Drops existing tables!)

### Issue 6: Date Format Errors

**Error:** `Failed to parse field [createdAt]`

**Solution:**
This was fixed by adding `@JsonFormat` annotation. Ensure you have the latest code:
```java
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime createdAt;
```

---

## Production Deployment

### Pre-Production Checklist

- [ ] Change all default passwords
- [ ] Generate strong JWT secret
- [ ] Enable Elasticsearch security (xpack.security.enabled=true)
- [ ] Configure HTTPS/SSL
- [ ] Set up database backups
- [ ] Configure logging (file-based, rotation)
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure firewall rules
- [ ] Test disaster recovery procedures
- [ ] Document rollback procedures

### Production Configuration

```properties
# Production application.properties

# Profiles
spring.profiles.active=prod

# Database - Use connection pool
spring.datasource.url=jdbc:mysql://prod-db-server:3306/global_search_db?useSSL=true
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=20

# Elasticsearch - Use cluster
spring.elasticsearch.uris=http://es-node1:9200,http://es-node2:9200,http://es-node3:9200

# Security
jwt.secret=${JWT_SECRET}  # From environment variable

# Logging
logging.level.root=WARN
logging.level.com.globalsearch=INFO
logging.file.name=/var/log/global-search/application.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# Actuator (for monitoring)
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```

### Deployment Methods

#### 1. Standalone JAR
```bash
# Create systemd service (Linux)
sudo nano /etc/systemd/system/global-search.service

[Unit]
Description=Global Search Application
After=mysql.service elasticsearch.service

[Service]
User=appuser
ExecStart=/usr/bin/java -jar /opt/global-search/application.jar
SuccessExitStatus=143
Restart=always

[Install]
WantedBy=multi-user.target

# Enable and start
sudo systemctl enable global-search
sudo systemctl start global-search
```

#### 2. Docker (Planned)
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/global-search.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

```bash
# Build image
docker build -t global-search:latest .

# Run container
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JWT_SECRET=${JWT_SECRET} \
  --name global-search \
  global-search:latest
```

#### 3. Kubernetes (Future)
- Deploy as StatefulSet for Elasticsearch
- Deploy as Deployment for application (3 replicas)
- Use ConfigMaps for configuration
- Use Secrets for sensitive data
- Use Ingress for load balancing

### Production Monitoring

**Key Metrics to Monitor:**
- API response time (<1s target)
- Database connection pool usage
- Elasticsearch cluster health (green)
- JVM memory usage (<80%)
- Error rate (<1%)
- Request throughput

**Tools:**
- Prometheus + Grafana for metrics
- ELK Stack for centralized logging
- Sentry/Rollbar for error tracking

---

## Quick Start Commands

```bash
# Full deployment from scratch

# 1. Start MySQL
sudo systemctl start mysql

# 2. Start Elasticsearch
cd ~/elasticsearch-8.13.2 && ./bin/elasticsearch &

# 3. Clone and configure
git clone https://github.com/Zubeyr19/Global-Search-.git
cd Global-Search-
# Edit application.properties with your settings

# 4. Build and run
mvn clean package -DskipTests
java -jar target/global-search-1.0-SNAPSHOT.jar

# 5. Verify
curl http://localhost:8080/actuator/health
```

---

## Support & Resources

- **Documentation:** `/docs` folder
- **API Reference:** http://localhost:8080/swagger-ui.html
- **Issues:** https://github.com/Zubeyr19/Global-Search-/issues
- **Architecture:** See `docs/ARCHITECTURE.md`

---

**Document Version:** 1.0
**Last Updated:** 2025-10-28
**Author:** Development Team
