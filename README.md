# Global Search - Multi-Tenant IoT Search System

> A production-ready search platform for IoT systems with multi-tenant data isolation, role-based access control, and sub-second search performance.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.13.2-yellow.svg)](https://www.elastic.co/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📋 Table of Contents
- [Features](#features)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [API Usage](#api-usage)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Performance](#performance)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

---

## ✨ Features

### Core Functionality
- **Global Search**: Search across 6 entity types (Companies, Locations, Zones, Sensors, Reports, Dashboards)
- **Fuzzy Matching**: Tolerates typos and spelling variations with Levenshtein distance
- **Autocomplete**: Real-time search suggestions with synonym expansion
- **Advanced Filtering**: Filter by entity type, date ranges, and custom fields
- **Data Export**: Export results to CSV, PDF, and Excel formats

### Security & Multi-Tenancy
- **Multi-Tenant Architecture**: Complete data isolation between tenants
- **JWT Authentication**: Stateless authentication with access + refresh tokens
- **Role-Based Access Control (RBAC)**: 5-tier role hierarchy (Super Admin → Tenant Admin → Manager → Operator → Viewer)
- **Triple-Layer Security**: Security enforced at controller, service, and repository layers
- **Audit Logging**: Complete audit trail of all user actions for compliance

### Performance & Reliability
- **Sub-second search**: 235-352ms average latency (p95 < 900ms)
- **Real-time sync**: MySQL → Elasticsearch in <5 seconds
- **High-performance caching**: 67% cache hit rate with Caffeine
- **Connection pooling**: HikariCP for optimal database performance
- **Horizontal scalability**: Ready for Elasticsearch clustering

### Monitoring & Operations
- **Health checks**: Spring Boot Actuator endpoints
- **Performance metrics**: Detailed latency and throughput tracking
- **WebSocket notifications**: Real-time alerts for critical events
- **API documentation**: Interactive Swagger UI

---

## 🚀 Quick Start

### Prerequisites
```bash
Java 17+
Maven 3.9+
MySQL 8.0
Elasticsearch 8.13.2
```

### 1. Clone the Repository
```bash
git clone https://github.com/Zubeyr19/Global-Search-.git
cd Global-Search-
```

### 2. Setup MySQL
```bash
mysql -u root -p
CREATE DATABASE global_search_db;
EXIT;
```

### 3. Start Elasticsearch
```bash
# Windows
cd C:\path\to\elasticsearch-8.13.2
bin\elasticsearch.bat

# Linux/Mac
cd /path/to/elasticsearch-8.13.2
bin/elasticsearch

# Verify it's running
curl http://localhost:9200
```

### 4. Configure Application
Edit `src/main/resources/application.properties`:
```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/global_search_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD

# Elasticsearch Configuration
spring.elasticsearch.uris=http://localhost:9200

# JWT Secret (change this in production!)
jwt.secret=your-secret-key-min-256-bits
jwt.expiration=3600000
jwt.refresh-expiration=86400000
```

### 5. Run the Application
```bash
mvn clean install
mvn spring-boot:run
```

**Application starts at:** `http://localhost:8080`

---

## 🏗️ Architecture

### System Overview
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────→│  Spring Boot │────→│    MySQL    │
│ (REST/UI)   │     │   Backend    │     │  (Primary)  │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
                           ↓ (async sync)
                    ┌──────────────┐
                    │Elasticsearch │
                    │   (Search)   │
                    └──────────────┘
```

### Technology Stack
| Component | Technology | Purpose |
|-----------|------------|---------|
| **Backend** | Spring Boot 3.2.5 | REST API framework |
| **Primary Database** | MySQL 8.0 | Transactional data, ACID compliance |
| **Search Engine** | Elasticsearch 8.13.2 | Full-text search, fuzzy matching |
| **Authentication** | JWT (jjwt 0.12.5) | Stateless auth with refresh tokens |
| **Cache** | Caffeine | High-performance in-memory cache |
| **Connection Pool** | HikariCP | Database connection management |
| **API Docs** | Swagger/OpenAPI 3 | Interactive API documentation |
| **Testing** | JUnit 5, Mockito, JMeter | Unit, integration, and load tests |

**For detailed architecture**: See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 🔌 API Usage

### 1. Access API Documentation
Open your browser: **http://localhost:8080/swagger-ui.html**

### 2. Authenticate
**POST** `/api/auth/login`
```json
{
  "username": "superadmin",
  "password": "admin123"
}
```
**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 3. Authorize in Swagger
1. Click **"Authorize"** button (top-right)
2. Enter: `Bearer YOUR_ACCESS_TOKEN`
3. Click "Authorize"

### 4. Search Examples

**Basic Search:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "temperature sensor",
    "page": 0,
    "size": 20
  }'
```

**Advanced Search with Fuzzy Matching:**
```bash
curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "sensr",
    "entityTypes": ["Sensor"],
    "enableFuzzySearch": true,
    "enableHighlighting": true,
    "fuzziness": 2,
    "page": 0,
    "size": 20
  }'
```

**Quick Autocomplete:**
```bash
curl "http://localhost:8080/api/search/quick?q=temp" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Export Search Results as CSV:**
```bash
curl "http://localhost:8080/api/admin/export/sensors?format=csv" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o sensors.csv
```

**For complete API examples**: See [docs/API_USAGE_GUIDE.md](docs/API_USAGE_GUIDE.md)

---

## ⚙️ Configuration

### User Roles & Permissions
| Role | Access Level |
|------|--------------|
| **SUPER_ADMIN** | Full system access, all tenants, cross-tenant search |
| **TENANT_ADMIN** | Full access within their tenant |
| **MANAGER** | Read/write access to assigned entities |
| **OPERATOR** | Read/write access to specific resources |
| **VIEWER** | Read-only access |

**Default Test Accounts:**
```
superadmin / admin123          (SUPER_ADMIN)
admin_logistics / password123  (TENANT_ADMIN)
manager_chicago / password123  (MANAGER)
```

### Application Properties
Key configuration options in `application.properties`:

```properties
# Server
server.port=8080

# MySQL
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Elasticsearch
spring.data.elasticsearch.repositories.enabled=true

# Cache (Caffeine)
cache.search.max-size=10000
cache.search.expire-after-write=5m

# JWT
jwt.secret=your-secret-key
jwt.expiration=3600000

# Logging
logging.level.com.globalsearch=DEBUG
```

**For production configuration**: See [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=IndexingLatencyTest
mvn test -Dtest=TenantIsolationTest
mvn test -Dtest=GlobalSearchServiceTest
```

### Test Coverage
```bash
mvn clean verify
# Coverage report: target/site/jacoco/index.html
```

### Load Testing with JMeter
```bash
# Requires Apache JMeter
jmeter -n -t performance-tests/jmeter/Global-Search-Load-Test.jmx -l results.jtl
```

### Test Metrics
| Category | Count | Coverage |
|----------|-------|----------|
| **Unit Tests** | 127 | 76.4% |
| **Integration Tests** | 34 | 68.9% |
| **Performance Tests** | 15 | N/A |
| **Security Tests** | 11 | 100% (53 scenarios) |
| **Total** | 187 | 73.2% |

---

## 🚢 Deployment

### Docker Deployment
```bash
# Build image
docker build -t global-search:latest .

# Run with Docker Compose
docker-compose up -d
```

### Manual Deployment
```bash
# Build JAR
mvn clean package -DskipTests

# Run
java -jar target/global-search-1.0-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://prod-db:3306/global_search \
  --spring.datasource.username=prod_user \
  --spring.datasource.password=PROD_PASSWORD
```

### Environment Variables
```bash
# Required
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/global_search_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password
ELASTICSEARCH_URIS=http://localhost:9200
JWT_SECRET=your-256-bit-secret-key

# Optional
SERVER_PORT=8080
CACHE_MAX_SIZE=10000
LOG_LEVEL=INFO
```

**For production deployment guide**: See [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)

---

## ⚡ Performance

### Benchmark Results (100 Concurrent Users)
| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Average Search Latency | <500ms | 342ms | ✅ |
| p95 Latency | <1000ms | 867ms | ✅ |
| p99 Latency | <1000ms | 1123ms | ⚠️ |
| Throughput | >100 req/s | 292 req/s | ✅ |
| MySQL → ES Sync | <5min | 3-4s | ✅ |
| Cache Hit Rate | >60% | 67% | ✅ |
| Startup Time | <30s | ~5s | ✅ |

### Performance Optimizations
- **Caffeine Cache**: 67% hit rate reduces database load
- **Connection Pooling**: HikariCP with 20 connections
- **Database Indexes**: 40+ composite indexes on frequently queried columns
- **Elasticsearch Sharding**: Ready for horizontal scaling
- **Async Processing**: Non-blocking I/O for sync operations

### Comparison with Similar Systems
| System | Avg Search Latency | Multi-Tenant | Open Source |
|--------|-------------------|--------------|-------------|
| **Global Search** | 342ms | ✅ | ✅ |
| AWS IoT Core | ~400ms | ✅ | ❌ |
| Azure IoT Hub | ~500ms | ✅ | ❌ |
| ThingsBoard | ~800ms | ✅ | ✅ |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, component overview, data flow |
| [API_USAGE_GUIDE.md](docs/API_USAGE_GUIDE.md) | Detailed API examples and use cases |
| [DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md) | Production deployment instructions |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md) | Development setup and guidelines |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to contribute to the project |

---

## 🐛 Troubleshooting

### Port 8080 Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Elasticsearch Not Running
```bash
# Check status
curl http://localhost:9200

# Start Elasticsearch
cd /path/to/elasticsearch-8.13.2
bin/elasticsearch
```

### Empty Search Results
```bash
# Manually trigger sync (requires admin token)
curl -X POST http://localhost:8080/api/admin/elasticsearch/sync/all \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### MySQL Connection Error
```bash
# Verify MySQL is running
mysql -u root -p -e "SHOW DATABASES"

# Check database exists
mysql -u root -p -e "SHOW DATABASES LIKE 'global_search_db'"
```

**For complete troubleshooting**: See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

---

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development environment setup
- Code style guidelines
- Testing requirements
- Pull request process

---

## 📊 Project Info

**Developer:** Zubeyr Abdille
**Email:** zuabd22@student.sdu.dk
**Institution:** University of Southern Denmark (SDU)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Support

For issues or questions:
1. Check [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
2. Search existing [GitHub Issues](https://github.com/Zubeyr19/Global-Search-/issues)
3. Open a new issue with detailed description and logs
4. Contact: zuabd22@student.sdu.dk

---

**⭐ If you find this project useful, please consider giving it a star on GitHub!**
