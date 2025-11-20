# Global Search - Database Schema Documentation

## Table of Contents
1. [Overview](#overview)
2. [Database Design Principles](#database-design-principles)
3. [Entity Relationship Diagram](#entity-relationship-diagram)
4. [Table Definitions](#table-definitions)
5. [Indexes](#indexes)
6. [Relationships](#relationships)
7. [Data Migration](#data-migration)
8. [Backup and Recovery](#backup-and-recovery)
9. [Performance Tuning](#performance-tuning)

---

## Overview

### Database Information
- **Database Type:** MySQL 8.0
- **Character Set:** UTF-8 (utf8mb4)
- **Collation:** utf8mb4_unicode_ci
- **Storage Engine:** InnoDB
- **Estimated Size (Production):** ~1TB
- **Monthly Growth:** 15-30GB

### Design Goals
- Multi-tenant data isolation
- High query performance
- Referential integrity
- Audit trail support
- Scalability to 5TB

---

## Database Design Principles

### Multi-Tenancy
Every table includes a `tenant_id` column for data isolation:
- Enforced at application layer
- Indexed for fast filtering
- Foreign keys respect tenant boundaries

### Normalization
- Database follows 3rd Normal Form (3NF)
- Denormalization used selectively for performance (e.g., audit logs)
- Junction tables for many-to-many relationships

### Indexing Strategy
- Primary keys (auto-increment BIGINT)
- Foreign keys
- Tenant ID columns
- Frequently queried columns
- Composite indexes for common query patterns

---

## Entity Relationship Diagram

```
┌─────────────┐
│   Company   │
│ (tenant)    │
└──────┬──────┘
       │ 1
       │
       │ *
┌──────┴──────┐
│  Location   │
└──────┬──────┘
       │ 1
       │
       │ *
┌──────┴──────┐
│    Zone     │
└──────┬──────┘
       │ 1
       │
       │ *
┌──────┴──────┐
│   Sensor    │
└──────┬──────┘
       │ 1
       │
       │ *
┌──────┴──────┐
│ Sensor Data │
└─────────────┘

┌─────────────┐      ┌─────────────┐
│    User     │──────│ User Roles  │
└──────┬──────┘  *   └─────────────┘
       │
       │ *
       │ 1
┌──────┴──────┐
│  AuditLog   │
└─────────────┘

┌─────────────┐
│   Policy    │
└─────────────┘

┌─────────────┐
│  Dashboard  │
└─────────────┘

┌─────────────┐
│   Report    │
└─────────────┘
```

---

## Table Definitions

### 1. users

**Description:** Stores user authentication and profile information

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt hashed',
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    tenant_id VARCHAR(50) NOT NULL,
    company_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_id (tenant_id),
    INDEX idx_company_id (company_id),
    INDEX idx_username (username),
    INDEX idx_email (email),

    CONSTRAINT fk_user_company FOREIGN KEY (company_id)
        REFERENCES companies(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Columns:**
- `id`: Primary key
- `username`: Unique username (3-50 chars, alphanumeric + dots/underscores/hyphens)
- `password`: BCrypt hash (60 chars)
- `email`: User email address
- `tenant_id`: Multi-tenant isolation key
- `company_id`: Associated company (nullable)
- `enabled`: Account active status
- `last_login`: Last successful login timestamp

### 2. user_roles

**Description:** Stores user role assignments (Element Collection)

```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,

    PRIMARY KEY (user_id, role),
    INDEX idx_user_id (user_id),

    CONSTRAINT fk_userroles_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Roles:**
- `SUPER_ADMIN`: System-wide administrator
- `TENANT_ADMIN`: Tenant administrator
- `MANAGER`: Department/location manager
- `OPERATOR`: Operations personnel
- `VIEWER`: Read-only access

### 3. companies

**Description:** Top-level tenant entities representing organizations

```sql
CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    industry VARCHAR(100),
    city VARCHAR(100),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    max_users INT DEFAULT 100,
    max_locations INT DEFAULT 50,
    max_sensors INT DEFAULT 1000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_name (name),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_industry (industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Status Values:**
- `ACTIVE`: Company is active
- `INACTIVE`: Temporarily disabled
- `SUSPENDED`: Suspended due to policy violation

### 4. locations

**Description:** Physical sites or facilities

```sql
CREATE TABLE locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_company_id (company_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_city_country (city, country),

    CONSTRAINT fk_location_company FOREIGN KEY (company_id)
        REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5. zones

**Description:** Logical areas within locations

```sql
CREATE TABLE zones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    type VARCHAR(50),
    floor_number INT,
    area_sqm DECIMAL(10, 2),
    temperature_min DECIMAL(5, 2),
    temperature_max DECIMAL(5, 2),
    humidity_min DECIMAL(5, 2),
    humidity_max DECIMAL(5, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_location_id (location_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_type (type),
    INDEX idx_status (status),

    CONSTRAINT fk_zone_location FOREIGN KEY (location_id)
        REFERENCES locations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 6. sensors

**Description:** IoT devices and sensors

```sql
CREATE TABLE sensors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(100) NOT NULL UNIQUE,
    sensor_type VARCHAR(50) NOT NULL,
    zone_id BIGINT,
    tenant_id VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    firmware_version VARCHAR(50),
    installation_date DATE,
    last_maintenance_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_reading DECIMAL(10, 2),
    last_reading_time TIMESTAMP NULL,
    battery_level INT,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE INDEX idx_serial_number (serial_number),
    INDEX idx_zone_id (zone_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_sensor_type (sensor_type),
    INDEX idx_status (status),
    INDEX idx_tenant_type (tenant_id, sensor_type),

    CONSTRAINT fk_sensor_zone FOREIGN KEY (zone_id)
        REFERENCES zones(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Sensor Types:**
- `TEMPERATURE`
- `HUMIDITY`
- `PRESSURE`
- `MOTION`
- `AIR_QUALITY`
- `LIGHT`
- `POWER`
- `WATER`

### 7. sensor_data

**Description:** Time-series sensor readings

```sql
CREATE TABLE sensor_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id BIGINT NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    value DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20),
    quality VARCHAR(20) DEFAULT 'GOOD',
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_sensor_id (sensor_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_sensor_timestamp (sensor_id, timestamp),

    CONSTRAINT fk_sensordata_sensor FOREIGN KEY (sensor_id)
        REFERENCES sensors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
PARTITION BY RANGE (YEAR(timestamp)) (
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

**Note:** Partitioned by year for performance

### 8. audit_logs

**Description:** Comprehensive audit trail for compliance

```sql
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    tenant_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_url VARCHAR(500),
    http_method VARCHAR(10),
    http_status INT,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_tenant_timestamp (tenant_id, timestamp),

    CONSTRAINT fk_auditlog_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Audit Actions:**
- `LOGIN`, `LOGOUT`, `LOGIN_FAILED`
- `CREATE`, `UPDATE`, `DELETE`
- `SEARCH`, `EXPORT`, `SYNC`

### 9. policies

**Description:** Access control policies

```sql
CREATE TABLE policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    rules TEXT NOT NULL COMMENT 'JSON format',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_id (tenant_id),
    INDEX idx_role (role),
    INDEX idx_active (active),

    CONSTRAINT fk_policy_creator FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 10. reports

**Description:** Generated reports

```sql
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    parameters TEXT COMMENT 'JSON format',
    file_path VARCHAR(500),
    file_format VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT,
    generated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_id (tenant_id),
    INDEX idx_report_type (report_type),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),

    CONSTRAINT fk_report_creator FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 11. dashboards

**Description:** User dashboards and visualizations

```sql
CREATE TABLE dashboards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tenant_id VARCHAR(50) NOT NULL,
    dashboard_type VARCHAR(50),
    configuration TEXT COMMENT 'JSON format',
    is_shared BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    access_count INT DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_tenant_id (tenant_id),
    INDEX idx_dashboard_type (dashboard_type),
    INDEX idx_created_by (created_by),
    INDEX idx_is_shared (is_shared),

    CONSTRAINT fk_dashboard_creator FOREIGN KEY (created_by)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## Indexes

### Primary Indexes (Auto-created)
- Primary keys on all tables
- Unique constraints on usernames, emails, serial numbers

### Performance Indexes
```sql
-- Multi-tenant queries
CREATE INDEX idx_tenant_company ON companies(tenant_id);
CREATE INDEX idx_tenant_location ON locations(tenant_id);
CREATE INDEX idx_tenant_sensor ON sensors(tenant_id);

-- Common search patterns
CREATE INDEX idx_sensor_type_status ON sensors(sensor_type, status);
CREATE INDEX idx_location_city_country ON locations(city, country);

-- Audit log queries
CREATE INDEX idx_audit_user_time ON audit_logs(user_id, timestamp);
CREATE INDEX idx_audit_action_time ON audit_logs(action, timestamp);

-- Time-series queries
CREATE INDEX idx_sensordata_time ON sensor_data(sensor_id, timestamp DESC);
```

---

## Relationships

### One-to-Many
- Company → Locations
- Company → Users
- Location → Zones
- Zone → Sensors
- Sensor → Sensor Data
- User → Audit Logs
- User → Reports
- User → Dashboards

### Many-to-Many
- User ↔ Roles (via user_roles junction table)

### Cascade Rules
- **ON DELETE CASCADE**: Child records deleted when parent deleted
  - Company deletion → Locations deleted
  - Location deletion → Zones deleted
  - Zone deletion → Sensors unlinked (SET NULL)
  - Sensor deletion → Sensor Data deleted

- **ON DELETE SET NULL**: Foreign key set to NULL
  - User deletion → Audit logs retained (user_id = NULL)
  - User deletion → Reports retained (created_by = NULL)

---

## Data Migration

### Initial Schema Creation
```bash
# Auto-created by Hibernate
spring.jpa.hibernate.ddl-auto=update

# Or manual creation
mysql -u root -p global_search_db < schema.sql
```

### Adding Indexes (Post-deployment)
```sql
-- Add index without locking table
CREATE INDEX CONCURRENTLY idx_new_index ON table_name(column_name);
```

### Schema Versioning
Use Flyway or Liquibase for production:
```sql
-- V1__initial_schema.sql
-- V2__add_sensor_battery_level.sql
-- V3__add_zone_temperature_limits.sql
```

---

## Backup and Recovery

### Backup Strategy

**Full Backup (Daily)**
```bash
mysqldump -u root -p \
  --single-transaction \
  --routines \
  --triggers \
  global_search_db > backup_$(date +%Y%m%d).sql

# Compress
gzip backup_$(date +%Y%m%d).sql
```

**Incremental Backup (Hourly)**
```bash
# Enable binary logging
mysql> SET GLOBAL binlog_format = 'ROW';

# Backup binary logs
mysqlbinlog mysql-bin.000001 > incremental_backup.sql
```

**Selective Table Backup**
```bash
mysqldump -u root -p global_search_db \
  audit_logs sensor_data > sensitive_data_backup.sql
```

### Recovery

**Full Restore**
```bash
mysql -u root -p global_search_db < backup_20251109.sql
```

**Point-in-Time Recovery**
```bash
# Restore full backup
mysql -u root -p global_search_db < backup_20251109.sql

# Apply binary logs up to specific time
mysqlbinlog --stop-datetime="2025-11-09 14:30:00" \
  mysql-bin.000001 | mysql -u root -p global_search_db
```

---

## Performance Tuning

### Query Optimization

**Use EXPLAIN**
```sql
EXPLAIN SELECT * FROM sensors
WHERE tenant_id = 'TENANT_A' AND sensor_type = 'TEMPERATURE';
```

**Analyze Slow Queries**
```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- Check slow queries
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
```

### Index Optimization

**Check Index Usage**
```sql
SELECT * FROM information_schema.STATISTICS
WHERE table_schema = 'global_search_db';

-- Find unused indexes
SELECT * FROM sys.schema_unused_indexes
WHERE object_schema = 'global_search_db';
```

### Table Optimization

**Analyze Tables**
```sql
ANALYZE TABLE companies, locations, zones, sensors;
```

**Optimize Tables**
```sql
OPTIMIZE TABLE sensor_data;
```

### Configuration Tuning

**my.cnf / my.ini:**
```ini
[mysqld]
# Buffer pool (70-80% of RAM for dedicated server)
innodb_buffer_pool_size = 8G

# Log file size
innodb_log_file_size = 512M

# Query cache (deprecated in MySQL 8.0, use application cache)
# query_cache_size = 0

# Max connections
max_connections = 200

# Connection timeout
wait_timeout = 600

# Temp table size
tmp_table_size = 64M
max_heap_table_size = 64M
```

---

## Maintenance Tasks

### Regular Maintenance

**Weekly:**
- Analyze tables
- Check for fragmentation
- Review slow query log

**Monthly:**
- Optimize tables
- Purge old audit logs (>90 days)
- Review and update indexes
- Archive old sensor_data

**Quarterly:**
- Full database backup verification
- Security audit
- Capacity planning review

### Archiving Strategy

**Archive Old Sensor Data**
```sql
-- Create archive table
CREATE TABLE sensor_data_archive LIKE sensor_data;

-- Move old data (>1 year)
INSERT INTO sensor_data_archive
SELECT * FROM sensor_data
WHERE timestamp < DATE_SUB(NOW(), INTERVAL 1 YEAR);

-- Delete from main table
DELETE FROM sensor_data
WHERE timestamp < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

## Resources

- [MySQL 8.0 Documentation](https://dev.mysql.com/doc/refman/8.0/en/)
- [InnoDB Storage Engine](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Maintained By:** Development Team
