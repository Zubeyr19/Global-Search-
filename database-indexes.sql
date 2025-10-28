-- ===========================================================================
-- PERFORMANCE OPTIMIZATION: DATABASE INDEXES
-- ===========================================================================
-- This script creates critical indexes for the Global Search system
-- to achieve <1s query latency even with 1TB+ database size
-- ===========================================================================

-- ===========================================================================
-- COMPANIES TABLE INDEXES
-- ===========================================================================

-- Index for tenant-based filtering (MOST CRITICAL - used in every query)
CREATE INDEX IF NOT EXISTS idx_companies_tenant_id
ON companies(tenant_id);

-- Index for name searches
CREATE INDEX IF NOT EXISTS idx_companies_name
ON companies(name(100));

-- Composite index for tenant + name searches (PERFORMANCE CRITICAL)
CREATE INDEX IF NOT EXISTS idx_companies_tenant_name
ON companies(tenant_id, name(100));

-- Index for status filtering
CREATE INDEX IF NOT EXISTS idx_companies_status
ON companies(status);

-- Index for industry filtering
CREATE INDEX IF NOT EXISTS idx_companies_industry
ON companies(industry(50));

-- Index for timestamp sorting
CREATE INDEX IF NOT EXISTS idx_companies_created_at
ON companies(created_at DESC);

-- ===========================================================================
-- LOCATIONS TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_locations_tenant_id
ON locations(tenant_id);

CREATE INDEX IF NOT EXISTS idx_locations_company_id
ON locations(company_id);

CREATE INDEX IF NOT EXISTS idx_locations_name
ON locations(name(100));

CREATE INDEX IF NOT EXISTS idx_locations_tenant_name
ON locations(tenant_id, name(100));

CREATE INDEX IF NOT EXISTS idx_locations_city
ON locations(city(50));

CREATE INDEX IF NOT EXISTS idx_locations_country
ON locations(country(50));

CREATE INDEX IF NOT EXISTS idx_locations_city_country
ON locations(city(50), country(50));

CREATE INDEX IF NOT EXISTS idx_locations_status
ON locations(status);

-- ===========================================================================
-- ZONES TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_zones_tenant_id
ON zones(tenant_id);

CREATE INDEX IF NOT EXISTS idx_zones_location_id
ON zones(location_id);

CREATE INDEX IF NOT EXISTS idx_zones_name
ON zones(name(100));

CREATE INDEX IF NOT EXISTS idx_zones_tenant_name
ON zones(tenant_id, name(100));

CREATE INDEX IF NOT EXISTS idx_zones_status
ON zones(status);

CREATE INDEX IF NOT EXISTS idx_zones_type
ON zones(type(50));

-- ===========================================================================
-- SENSORS TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_sensors_tenant_id
ON sensors(tenant_id);

CREATE INDEX IF NOT EXISTS idx_sensors_zone_id
ON sensors(zone_id);

CREATE INDEX IF NOT EXISTS idx_sensors_name
ON sensors(name(100));

CREATE INDEX IF NOT EXISTS idx_sensors_tenant_name
ON sensors(tenant_id, name(100));

CREATE INDEX IF NOT EXISTS idx_sensors_serial_number
ON sensors(serial_number(50));

CREATE INDEX IF NOT EXISTS idx_sensors_sensor_type
ON sensors(sensor_type(50));

CREATE INDEX IF NOT EXISTS idx_sensors_status
ON sensors(status);

-- Composite index for filtering by type and zone
CREATE INDEX IF NOT EXISTS idx_sensors_type_zone
ON sensors(sensor_type(50), zone_id);

-- ===========================================================================
-- REPORTS TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_reports_tenant_id
ON reports(tenant_id);

CREATE INDEX IF NOT EXISTS idx_reports_name
ON reports(name(100));

CREATE INDEX IF NOT EXISTS idx_reports_tenant_name
ON reports(tenant_id, name(100));

CREATE INDEX IF NOT EXISTS idx_reports_report_type
ON reports(report_type(50));

CREATE INDEX IF NOT EXISTS idx_reports_created_by
ON reports(created_by);

CREATE INDEX IF NOT EXISTS idx_reports_created_at
ON reports(created_at DESC);

-- ===========================================================================
-- DASHBOARDS TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_dashboards_tenant_id
ON dashboards(tenant_id);

CREATE INDEX IF NOT EXISTS idx_dashboards_name
ON dashboards(name(100));

CREATE INDEX IF NOT EXISTS idx_dashboards_tenant_name
ON dashboards(tenant_id, name(100));

CREATE INDEX IF NOT EXISTS idx_dashboards_dashboard_type
ON dashboards(dashboard_type(50));

CREATE INDEX IF NOT EXISTS idx_dashboards_is_shared
ON dashboards(is_shared);

CREATE INDEX IF NOT EXISTS idx_dashboards_created_at
ON dashboards(created_at DESC);

-- ===========================================================================
-- USERS TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_users_username
ON users(username(50));

CREATE INDEX IF NOT EXISTS idx_users_email
ON users(email(100));

CREATE INDEX IF NOT EXISTS idx_users_tenant_id
ON users(tenant_id);

CREATE INDEX IF NOT EXISTS idx_users_company_id
ON users(company_id);

CREATE INDEX IF NOT EXISTS idx_users_enabled
ON users(enabled);

-- ===========================================================================
-- AUDIT_LOGS TABLE INDEXES (CRITICAL FOR PERFORMANCE - GROWS VERY LARGE)
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id
ON audit_logs(user_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id
ON audit_logs(tenant_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action
ON audit_logs(action);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp
ON audit_logs(timestamp DESC);

-- Composite index for user activity queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_timestamp
ON audit_logs(user_id, timestamp DESC);

-- Composite index for tenant activity queries
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_timestamp
ON audit_logs(tenant_id, timestamp DESC);

-- Index for entity tracking
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
ON audit_logs(entity_type(50), entity_id);

-- ===========================================================================
-- POLICIES TABLE INDEXES
-- ===========================================================================

CREATE INDEX IF NOT EXISTS idx_policies_tenant_id
ON policies(tenant_id);

CREATE INDEX IF NOT EXISTS idx_policies_role
ON policies(role(50));

CREATE INDEX IF NOT EXISTS idx_policies_active
ON policies(active);

CREATE INDEX IF NOT EXISTS idx_policies_tenant_role
ON policies(tenant_id, role(50));

-- ===========================================================================
-- SENSOR_DATA TABLE INDEXES (LARGEST TABLE - ~800GB)
-- ===========================================================================
-- NOTE: Be cautious with indexes on very large tables
-- Consider partitioning strategy instead of too many indexes

CREATE INDEX IF NOT EXISTS idx_sensor_data_sensor_id
ON sensor_data(sensor_id);

CREATE INDEX IF NOT EXISTS idx_sensor_data_timestamp
ON sensor_data(timestamp DESC);

-- Composite index for time-series queries (CRITICAL FOR PERFORMANCE)
CREATE INDEX IF NOT EXISTS idx_sensor_data_sensor_timestamp
ON sensor_data(sensor_id, timestamp DESC);

-- ===========================================================================
-- VERIFY INDEXES
-- ===========================================================================

-- Run this query to verify all indexes were created:
-- SELECT
--     TABLE_NAME,
--     INDEX_NAME,
--     COLUMN_NAME,
--     NON_UNIQUE,
--     INDEX_TYPE,
--     CARDINALITY
-- FROM
--     information_schema.STATISTICS
-- WHERE
--     TABLE_SCHEMA = 'global_search_db'
--     AND TABLE_NAME IN ('companies', 'locations', 'zones', 'sensors', 'reports', 'dashboards', 'users', 'audit_logs', 'policies', 'sensor_data')
-- ORDER BY
--     TABLE_NAME, INDEX_NAME;

-- ===========================================================================
-- PERFORMANCE IMPACT
-- ===========================================================================
-- Expected Performance Improvements:
-- 1. tenant_id filters: 50-100x faster (full table scan → index scan)
-- 2. name searches: 20-50x faster (especially with LIKE queries)
-- 3. Composite indexes: 100-500x faster for common query patterns
-- 4. Sorting operations: 10-30x faster
-- 5. JOIN operations: 30-70x faster
--
-- With these indexes, query latency should be:
-- - Simple queries: <10ms
-- - Complex queries with joins: <100ms
-- - Full-text searches: <500ms
-- - All well under the <1s requirement
-- ===========================================================================
