# ER Diagram - Chen Notation

## Single Complete Diagram

**File:** `ERDiagram_Chen.puml`

## What's Included

### 10 Entities (Rectangles):
1. **COMPANY** - Multi-tenant root entity
2. **USER** - System users with roles
3. **LOCATION** - Physical locations
4. **ZONE** - Areas within locations
5. **SENSOR** - IoT devices
6. **SENSOR_DATA** - Sensor readings (800GB+ table)
7. **DASHBOARD** - User dashboards
8. **REPORT** - Generated reports
9. **AUDIT_LOG** - Activity tracking
10. **POLICY** - Access control rules

### 8 Relationships (Diamonds):
1. **has** - Company (1) : (N) Location
2. **contains** - Location (1) : (N) Zone
3. **includes** - Zone (1) : (N) Sensor
4. **generates** - Sensor (1) : (N) SensorData
5. **employs** - Company (1) : (N) User
6. **owns** - User (1) : (N) Dashboard
7. **creates** - User (1) : (N) Report
8. **logs** - User (1) : (N) AuditLog

### All Attributes (Ovals):
- Each entity has 7-12 attributes
- Primary keys are **underlined** (<u>id</u>)
- Includes tenant_id for multi-tenancy
- Status fields, timestamps, foreign keys

## Chen Notation Elements

- **Rectangles** = Entities
- **Diamonds** = Relationships
- **Ovals** = Attributes
- **Underlined text** = Primary Keys
- **1:N** = One-to-Many cardinality

## System Architecture Highlights

1. **Multi-tenant** via tenant_id in COMPANY
2. **Hierarchical data** - Company → Location → Zone → Sensor → SensorData
3. **Role-based access** stored in USER.roles
4. **Audit compliance** through AUDIT_LOG
5. **Denormalized** SENSOR_DATA for performance

## To View

1. Open in PlantUML plugin (VSCode/IntelliJ)
2. Or use: http://www.plantuml.com/plantuml
3. Export as PNG/SVG for your thesis
