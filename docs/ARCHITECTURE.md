# Global Search System - Architecture Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Component Architecture](#component-architecture)
4. [Data Flow](#data-flow)
5. [Security Architecture](#security-architecture)
6. [Database Schema](#database-schema)
7. [Technology Stack](#technology-stack)
8. [Deployment Architecture](#deployment-architecture)

---

## System Overview

The Global Search System is a multi-tenant IoT search platform designed to provide fast, secure, and scalable search capabilities across multiple entity types including Companies, Locations, Zones, Sensors, Reports, and Dashboards.

### Key Features
- **Multi-tenant Data Isolation** - Strict tenant-level data segregation
- **Document-Level Security** - Fine-grained access control in Elasticsearch
- **Role-Based Access Control (RBAC)** - 5 distinct user roles
- **Real-time Synchronization** - Automatic MySQL → Elasticsearch sync
- **Sub-second Search** - < 1s query response time
- **Audit Logging** - GDPR-compliant activity tracking

### System Requirements
- **Database Size:** ~1TB (production)
- **Growth Rate:** 15-30GB/month
- **Performance:** <1s search latency
- **Indexing Latency:** <5 minutes
- **Scalability:** Support up to 5TB with <20% degradation

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐    │
│  │ Web App  │   │  Mobile  │   │   API    │   │  Admin   │    │
│  │ (React)  │   │   App    │   │ Clients  │   │  Portal  │    │
│  └─────┬────┘   └─────┬────┘   └─────┬────┘   └─────┬────┘    │
│        │              │              │              │          │
└────────┼──────────────┼──────────────┼──────────────┼──────────┘
         │              │              │              │
         └──────────────┴──────────────┴──────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY LAYER                           │
│  ┌────────────────────────────────────────────────────────┐    │
│  │               Spring Boot REST API (Port 8080)          │    │
│  │                                                          │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │    │
│  │  │ JWT Filter   │→│ Rate Limiter │→│  Security    │ │    │
│  │  │              │  │(100 req/min) │  │   Filter    │ │    │
│  │  └──────────────┘  └──────────────┘  └──────────────┘ │    │
│  └────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CONTROLLER LAYER                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Auth       │  │   Search     │  │    Admin     │          │
│  │ Controller   │  │  Controller  │  │  Controller  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
└─────────┼─────────────────┼─────────────────┼───────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Auth        │  │   Search     │  │   Policy     │          │
│  │  Service     │  │   Service    │  │   Service    │          │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤          │
│  │  Audit       │  │   Export     │  │   Sync       │          │
│  │  Service     │  │   Service    │  │   Service    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                   REPOSITORY LAYER                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │     JPA      │  │Elasticsearch │  │   Policy     │          │
│  │ Repositories │  │ Repositories │  │  Repository  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
   ┌──────┴──────┐    ┌──────┴──────┐   ┌──────┴──────┐
   ▼             ▼    ▼             ▼   ▼             ▼
┌──────────┐ ┌──────────┐       ┌──────────┐   ┌──────────┐
│  MySQL   │ │  Cache   │       │Elastic-  │   │ WebSocket│
│  8.0     │ │(Caffeine)│       │ search   │   │  Broker  │
│  (1TB)   │ │          │       │  8.13.2  │   │          │
└──────────┘ └──────────┘       └──────────┘   └──────────┘
│                                │
│    ┌───────────────────────────┴───┐
│    ▼                               ▼
│ ┌───────────────┐           ┌────────────────┐
│ │ Entity Sync   │           │   Audit Logs   │
│ │  Listener     │           │   (async)      │
│ │  (real-time)  │           │                │
│ └───────────────┘           └────────────────┘
└─────────────────────────────────────────────────
```

---

## Component Architecture

### 1. API Layer Components

#### Authentication & Authorization
- **JWT Token Provider** - Generates and validates JWT tokens
- **Security Filter Chain** - Spring Security configuration
- **Role-Based Access Control** - 5 roles: SUPER_ADMIN, TENANT_ADMIN, MANAGER, OPERATOR, VIEWER

#### Rate Limiting
- **Implementation:** In-memory (Caffeine)
- **Limit:** 100 requests/minute per IP
- **Production Note:** Migrate to Redis for distributed systems

### 2. Service Layer Components

#### SearchService
- **Purpose:** Core search logic
- **Features:**
  - Multi-entity search (6 types)
  - Tenant isolation
  - Role-based filtering
  - Fuzzy matching
  - Synonym support
  - Result highlighting

#### ElasticsearchSyncService
- **Purpose:** Data synchronization
- **Features:**
  - Startup bulk sync
  - Real-time incremental sync
  - Manual sync endpoints
  - Error handling & logging

#### AuditService
- **Purpose:** Activity tracking
- **Features:**
  - Async logging (non-blocking)
  - GDPR-compliant storage
  - User activity tracking
  - Search query logging

#### ExportService
- **Purpose:** Data export
- **Formats:** CSV, PDF, Excel
- **Use Cases:** Reports, audit logs, search results

### 3. Data Layer Components

#### JPA Repositories
- **Purpose:** MySQL data access
- **Entities:** User, Company, Location, Zone, Sensor, Report, Dashboard, Policy
- **Features:**
  - Optimized queries
  - Batch operations
  - Connection pooling (HikariCP)

#### Elasticsearch Repositories
- **Purpose:** Full-text search
- **Documents:** Mirror JPA entities
- **Features:**
  - Document-level security
  - Multi-field indexing
  - Synonym filters

#### Cache Layer
- **Implementation:** Caffeine (in-memory)
- **Cached Objects:**
  - User sessions
  - Search results (short TTL)
  - Tenant metadata
- **Production Note:** Migrate to Redis

---

## Data Flow

### 1. Search Request Flow

```
1. Client → [POST /api/search] → API Gateway
2. API Gateway → JWT Validation → Rate Limiter → Security Filter
3. Security Filter → Extract User (tenant, role) → SearchController
4. SearchController → SearchService.globalSearch()
5. SearchService → Apply tenant filter + role permissions
6. SearchService → Build Elasticsearch query
7. SearchService → Execute query → Elasticsearch
8. Elasticsearch → Return results
9. SearchService → Transform to DTO → Filter sensitive data
10. SearchController → Return JSON → Client
```

### 2. Data Synchronization Flow

```
CREATE/UPDATE/DELETE in MySQL:
1. JPA Entity Change → @PrePersist/@PostUpdate/@PreRemove
2. EntitySyncListener → Triggered
3. EntitySyncListener → ElasticsearchSyncService
4. ElasticsearchSyncService → Transform Entity → Document
5. ElasticsearchSyncService → Save to Elasticsearch (async)
6. Elasticsearch → Index updated
7. Document searchable in <5 seconds
```

### 3. Authentication Flow

```
1. Client → [POST /api/auth/login] {username, password}
2. AuthController → AuthService.authenticate()
3. AuthService → Validate credentials (BCrypt)
4. AuthService → Load user + roles + tenant
5. AuthService → JwtTokenProvider.generateToken()
6. JwtTokenProvider → Create JWT (tenant, roles, expiry)
7. AuthService → Return {accessToken, refreshToken, user}
8. Client → Store token → Include in future requests
```

---

## Security Architecture

### 1. Multi-Tenant Isolation

#### Database Level
- **tenant_id column** in all tables
- **Mandatory filters** on all queries
- **Foreign key constraints** enforce boundaries

#### Elasticsearch Level
- **tenant_id field** in all documents
- **Document-level security** via search queries
- **Automatic filtering** in SearchService

### 2. Role-Based Access Control (RBAC)

| Role | Permissions | Scope |
|------|------------|-------|
| SUPER_ADMIN | All operations, cross-tenant access | System-wide |
| TENANT_ADMIN | Manage users, policies, view all tenant data | Single tenant |
| MANAGER | Create/update entities, view reports | Department/location |
| OPERATOR | Update sensor data, create reports | Operational |
| VIEWER | Read-only access to assigned entities | Limited |

### 3. Authentication & Authorization

- **JWT Tokens:** HS256 algorithm, 24-hour expiry
- **Refresh Tokens:** 7-day expiry
- **Password Hashing:** BCrypt (strength 10)
- **Session Management:** Stateless (JWT-based)

### 4. API Security

- **HTTPS Only** (production)
- **CORS Configuration** - Whitelist specific origins
- **Rate Limiting** - Prevent abuse
- **Input Validation** - @Valid annotations
- **SQL Injection Protection** - Parameterized queries
- **XSS Protection** - Content escaping

---

## Database Schema

### Core Entities

```sql
-- Users table with tenant association
users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(255),  -- BCrypt hashed
    email VARCHAR(100),
    full_name VARCHAR(100),
    role VARCHAR(20),       -- ENUM
    tenant_id VARCHAR(50),  -- Multi-tenant key
    company_id BIGINT,      -- FK to companies
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
)

-- Companies (top-level tenant entities)
companies (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    tenant_id VARCHAR(50) UNIQUE,
    industry VARCHAR(100),
    status VARCHAR(20),     -- ACTIVE, INACTIVE, SUSPENDED
    max_users INT,
    max_locations INT,
    max_sensors INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_tenant (tenant_id)
)

-- Locations (physical sites)
locations (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    company_id BIGINT,      -- FK to companies
    address VARCHAR(500),
    city VARCHAR(100),
    country VARCHAR(100),
    tenant_id VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_company (company_id),
    INDEX idx_tenant (tenant_id)
)

-- Zones (areas within locations)
zones (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    location_id BIGINT,     -- FK to locations
    zone_type VARCHAR(50),
    tenant_id VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

-- Sensors (IoT devices)
sensors (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    sensor_type VARCHAR(50), -- TEMPERATURE, HUMIDITY, etc.
    zone_id BIGINT,          -- FK to zones
    status VARCHAR(20),
    last_reading DECIMAL,
    last_reading_time TIMESTAMP,
    tenant_id VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_zone (zone_id),
    INDEX idx_tenant (tenant_id)
)

-- Audit Logs (GDPR-compliant)
audit_logs (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    tenant_id VARCHAR(50),
    action VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP,
    details TEXT,
    INDEX idx_user (user_id),
    INDEX idx_tenant_timestamp (tenant_id, timestamp)
)

-- Policies (access control rules)
policies (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    tenant_id VARCHAR(50),
    role VARCHAR(20),
    rules TEXT,             -- JSON format
    active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
```

### Elasticsearch Indices

Each entity type has a corresponding Elasticsearch index:
- `companies` - Company documents
- `locations` - Location documents
- `zones` - Zone documents
- `sensors` - Sensor documents
- `reports` - Report documents
- `dashboards` - Dashboard documents

---

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.2.5
- **Java Version:** 17
- **Build Tool:** Maven 3.9+
- **ORM:** Hibernate 6.x (JPA)
- **Security:** Spring Security 6.x with JWT

### Data Layer
- **Primary Database:** MySQL 8.0
- **Search Engine:** Elasticsearch 8.13.2
- **Cache:** Caffeine (in-memory)
- **Connection Pool:** HikariCP

### Libraries
- **JWT:** io.jsonwebtoken 0.12.5
- **Lombok:** Code generation
- **MapStruct:** DTO mapping (if used)
- **Apache POI:** Excel export
- **iText:** PDF generation

### DevOps
- **Version Control:** Git
- **CI/CD:** (To be configured)
- **Containerization:** Docker (planned)
- **Monitoring:** (To be added - Prometheus/Grafana)

---

## Deployment Architecture

### Development Environment
```
[Developer Machine]
├── MySQL 8.0 (localhost:3306)
├── Elasticsearch 8.13.2 (localhost:9200)
└── Spring Boot App (localhost:8080)
```

### Production Environment (Recommended)
```
[Load Balancer (HAProxy/Nginx)]
    │
    ├─→ [App Server 1] Spring Boot + JVM
    ├─→ [App Server 2] Spring Boot + JVM
    └─→ [App Server 3] Spring Boot + JVM
         │
         ├─→ [MySQL Cluster]
         │    ├── Primary (Write)
         │    └── Replicas (Read)
         │
         ├─→ [Elasticsearch Cluster]
         │    ├── Master Nodes (3)
         │    ├── Data Nodes (5+)
         │    └── Client Nodes (2)
         │
         └─→ [Redis Cluster] (Cache + Session)
              ├── Primary
              └── Replicas
```

### Scaling Considerations

1. **Horizontal Scaling**
   - Add more application servers behind load balancer
   - Stateless design allows easy scaling

2. **Database Scaling**
   - MySQL: Read replicas for read-heavy workloads
   - Partitioning by tenant_id for large datasets

3. **Elasticsearch Scaling**
   - Shard by tenant_id
   - Increase data nodes for storage
   - Increase replica count for query performance

4. **Cache Scaling**
   - Migrate from Caffeine to Redis cluster
   - Shared cache across app instances

---

## Performance Optimization

### Applied Optimizations
✅ HikariCP connection pooling (20 max connections)
✅ Hibernate batch operations (batch_size=50)
✅ Elasticsearch connection timeout tuning
✅ Async audit logging (non-blocking)
✅ Result pagination (max 100 per page)
✅ Caffeine caching (short TTL)

### Monitoring Points
- Query response time (< 1s target)
- Database connection pool usage
- Elasticsearch cluster health
- JVM memory usage
- Cache hit ratio
- API error rates

---

## Compliance & Security

### GDPR Compliance
- **Right to Access:** Audit log exports
- **Right to Erasure:** Data deletion endpoints
- **Data Minimization:** Only necessary fields stored
- **Encryption:** Data at rest (MySQL) and in transit (HTTPS)

### Security Best Practices
- **Principle of Least Privilege:** Role-based access
- **Defense in Depth:** Multiple security layers
- **Secure by Default:** Strong password policies
- **Audit Everything:** Comprehensive logging

---

## Future Enhancements

1. **Microservices Architecture** - Split into auth, search, audit services
2. **Event-Driven Architecture** - Kafka for async communication
3. **GraphQL API** - Flexible query interface
4. **Machine Learning** - Search result relevance tuning
5. **Real-time Analytics** - Dashboard with live metrics

---

**Document Version:** 1.0
**Last Updated:** 2025-10-28
**Maintained By:** Development Team
