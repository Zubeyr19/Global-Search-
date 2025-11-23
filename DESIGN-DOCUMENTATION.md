# Design Chapter - Key Components to Highlight

## 1. System Architecture Overview

### High-Level Architecture Diagram
```
┌─────────────────────────────────────────────────────────────────┐
│                         USER LAYER                               │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │ Web Browser  │    │   Mobile     │    │  API Client  │     │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘     │
└─────────┼────────────────────┼────────────────────┼─────────────┘
          │                    │                    │
          └────────────────────┴────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Frontend (HTML)    │
                    │   - Login UI         │
                    │   - Search Interface │
                    │   - Results Display  │
                    └──────────┬───────────┘
                               │ HTTPS/REST
┌──────────────────────────────┼─────────────────────────────────┐
│                    ┌─────────▼──────────┐    APPLICATION       │
│                    │   API Gateway      │      LAYER           │
│                    │  (Spring Boot)     │                      │
│                    └─────────┬──────────┘                      │
│                              │                                  │
│         ┌────────────────────┼────────────────────┐            │
│         │                    │                    │            │
│  ┌──────▼────────┐  ┌────────▼──────┐  ┌────────▼──────┐    │
│  │ Security      │  │ Search         │  │ Business      │    │
│  │ - JWT Auth    │  │ Service        │  │ Logic         │    │
│  │ - RBAC        │  │ - Query        │  │ - Validation  │    │
│  │ - Multi-Tenant│  │ - Aggregation  │  │ - Rules       │    │
│  └───────────────┘  └────────┬───────┘  └───────┬───────┘    │
└─────────────────────────────┼──────────────────┼──────────────┘
                              │                  │
              ┌───────────────┴──────────────────┴──────┐
              │                                          │
┌─────────────▼─────────────┐        ┌─────────────────▼────────┐
│   PERSISTENCE LAYER       │        │   SEARCH LAYER           │
│                           │        │                          │
│  ┌────────────────────┐   │        │  ┌────────────────────┐ │
│  │   MySQL Database   │   │        │  │  Elasticsearch     │ │
│  │                    │   │◄───────┼──│    8.11.0          │ │
│  │ - Users            │   │  Sync  │  │                    │ │
│  │ - Companies        │   │        │  │ - Companies Index  │ │
│  │ - Locations        │   │        │  │ - Locations Index  │ │
│  │ - Zones            │   │        │  │ - Zones Index      │ │
│  │ - Sensors          │   │        │  │ - Sensors Index    │ │
│  │ - Reports          │   │        │  │ - Reports Index    │ │
│  │ - Dashboards       │   │        │  │ - Dashboards Index │ │
│  │ - Audit Logs       │   │        │  └────────────────────┘ │
│  └────────────────────┘   │        └──────────────────────────┘
└───────────────────────────┘
```

**Description for Design Chapter:**
"The system follows a layered architecture pattern with clear separation of concerns. The frontend communicates with the backend via RESTful APIs, while the backend integrates both MySQL for persistent storage and Elasticsearch for high-performance search capabilities."

---

## 2. Database Schema (Screenshot: MySQL Workbench or ER Diagram)

### Key Tables & Relationships

```
┌─────────────────────┐
│     companies       │
├─────────────────────┤
│ id (PK)             │
│ name                │
│ tenant_id (FK)      │
│ industry            │
│ status              │
└──────────┬──────────┘
           │ 1:N
           ▼
┌─────────────────────┐       ┌─────────────────────┐
│    locations        │       │      users          │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ company_id (FK)     │       │ username (UNIQUE)   │
│ tenant_id (FK)      │       │ password (hashed)   │
│ name                │       │ email               │
│ address             │       │ tenant_id (FK)      │
│ city                │       │ company_id (FK)     │
└──────────┬──────────┘       │ roles (enum)        │
           │ 1:N              └─────────────────────┘
           ▼
┌─────────────────────┐
│       zones         │
├─────────────────────┤
│ id (PK)             │
│ location_id (FK)    │
│ tenant_id (FK)      │
│ name                │
└──────────┬──────────┘
           │ 1:N
           ▼
┌─────────────────────┐
│      sensors        │
├─────────────────────┤
│ id (PK)             │
│ zone_id (FK)        │
│ tenant_id (FK)      │
│ serial_number       │
│ sensor_type         │
│ status              │
└─────────────────────┘
```

**Screenshot to take:** Your actual database schema from MySQL Workbench or a tool like dbdiagram.io

**Description:**
"The database follows a hierarchical multi-tenant model where Companies contain Locations, Locations contain Zones, and Zones contain Sensors. Each entity is tagged with a tenant_id for data isolation."

---

## 3. API Endpoints Structure (Screenshot: Postman or Swagger)

### Core Endpoints

```
Authentication Endpoints:
POST   /api/auth/login          - User login (returns JWT)
POST   /api/auth/refresh        - Refresh JWT token
POST   /api/auth/logout         - User logout

Search Endpoints:
POST   /api/search              - Global search across entities
GET    /api/search/suggestions  - Auto-complete suggestions

Entity Management:
GET    /api/companies           - List companies
POST   /api/companies           - Create company
PUT    /api/companies/{id}      - Update company
DELETE /api/companies/{id}      - Delete company

(Similar CRUD for locations, zones, sensors, reports, dashboards)

Admin Endpoints:
GET    /api/admin/users         - List all users
POST   /api/admin/users         - Create user
GET    /api/audit-logs          - View audit logs
```

**Screenshot to take:** Postman collection or Swagger UI showing these endpoints

**Description:**
"The REST API follows standard conventions with clear resource naming and HTTP methods. All endpoints except /login require JWT authentication."

---

## 4. Security Architecture

### Authentication Flow Diagram

```
┌─────────┐                                    ┌──────────────┐
│ Client  │                                    │   Backend    │
└────┬────┘                                    └──────┬───────┘
     │                                                │
     │  1. POST /api/auth/login                      │
     │     {username, password}                      │
     ├──────────────────────────────────────────────►│
     │                                                │
     │                                         2. Validate
     │                                            credentials
     │                                                │
     │  3. Return JWT Token                          │
     │     {accessToken, refreshToken}               │
     │◄──────────────────────────────────────────────┤
     │                                                │
     │  4. Store token in localStorage                │
     │                                                │
     │  5. Subsequent requests with token            │
     │     Authorization: Bearer <token>             │
     ├──────────────────────────────────────────────►│
     │                                                │
     │                                         6. Verify JWT
     │                                            Extract user
     │                                            Check roles
     │                                                │
     │  7. Return requested data                     │
     │◄──────────────────────────────────────────────┤
     │                                                │
```

**Screenshot to take:** The JWT token structure (decoded from jwt.io)

**Description:**
"Security is implemented using JWT (JSON Web Tokens) with role-based access control (RBAC). Each token contains user ID, tenant ID, and assigned roles for authorization decisions."

---

## 5. Multi-Tenant Data Isolation

```
TENANT_GLOBAL (Super Admin)
├── Can see ALL data
└── Manages system configuration

TENANT_LOGISTICS
├── User: admin (Tenant Admin)
│   ├── Can see: Companies, Locations, Zones, Sensors
│   └── Filtered by: tenant_id = 'TENANT_LOGISTICS'
└── User: user (Regular User)
    ├── Can see: Limited data
    └── Filtered by: tenant_id = 'TENANT_LOGISTICS'

TENANT_MANUFACTURING
└── Completely isolated from TENANT_LOGISTICS
```

**Screenshot to take:** Database query with WHERE tenant_id clause, or search results showing tenant isolation

**Description:**
"Multi-tenancy is enforced at both database and application levels. Every query automatically filters by tenant_id, ensuring users only access their organization's data."

---

## 6. Elasticsearch Index Mapping (Screenshot)

### Example: Company Index Mapping

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "description": { "type": "text" },
      "industry": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "status": { "type": "keyword" },
      "createdAt": { "type": "date" }
    }
  }
}
```

**Screenshot to take:**
```bash
curl http://localhost:9200/companies/_mapping
```

**Description:**
"Elasticsearch indices use optimized field mappings with full-text search on name/description fields and keyword indexing for filtering by tenant, status, and industry."

---

## 7. Search Flow Diagram

```
┌──────────┐  1. User types    ┌─────────────┐
│ Frontend │   "Global Logis"  │   Backend   │
└─────┬────┘                   └──────┬──────┘
      │                               │
      │  2. POST /api/search          │
      │     {query: "Global Logis"}   │
      ├──────────────────────────────►│
      │                               │
      │                        3. Build Query
      │                        ┌──────▼──────┐
      │                        │ Query Builder│
      │                        │ - Add tenant  │
      │                        │ - Add filters │
      │                        └──────┬───────┘
      │                               │
      │                        4. Query ES
      │                        ┌──────▼──────────┐
      │                        │  Elasticsearch   │
      │                        │  - Search all    │
      │                        │    indices       │
      │                        │  - Calculate     │
      │                        │    relevance     │
      │                        └──────┬───────────┘
      │                               │
      │                        5. Aggregate Results
      │                               │
      │  6. Return Results            │
      │     {results: [...]}          │
      │◄──────────────────────────────┤
      │                               │
      │  7. Display Results           │
      │                               │
```

---

## 8. Class Diagram (Key Classes)

```
┌─────────────────────────┐
│   SearchController      │
├─────────────────────────┤
│ + search()              │
│ + suggest()             │
└───────────┬─────────────┘
            │ uses
            ▼
┌─────────────────────────┐
│   SearchService         │
├─────────────────────────┤
│ - companyRepo           │
│ - locationRepo          │
│ - sensorRepo            │
├─────────────────────────┤
│ + globalSearch()        │
│ + searchCompanies()     │
│ + searchLocations()     │
│ + calculateRelevance()  │
└───────────┬─────────────┘
            │ uses
            ▼
┌─────────────────────────┐
│ CompanySearchRepository │
├─────────────────────────┤
│ + findByTenantId()      │
│ + findByNameContaining()│
└─────────────────────────┘
```

---

## 9. Screenshots to Take for Your Design Chapter

### Essential Screenshots:

1. **✓ Code Structure** (File Explorer)
   - Show `src/main/java` package structure
   - Highlight: controller, service, repository, entity folders

2. **✓ Database Schema** (MySQL Workbench)
   - Show ER diagram with all tables
   - Highlight foreign key relationships

3. **✓ API Testing** (Postman)
   - Login request/response
   - Search request/response with JWT token

4. **✓ Elasticsearch Indices** (Terminal)
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

5. **✓ JWT Token Decoded** (jwt.io)
   - Show token payload with user info, roles, tenant

6. **✓ Frontend UI**
   - Login page
   - Search interface
   - Search results

7. **✓ Application.properties** (Configuration)
   - Show database config
   - Elasticsearch config
   - JWT settings

8. **✓ Spring Boot Startup Logs**
   - Show successful startup
   - Data synchronization logs

---

## 10. Key Design Decisions to Highlight

### A. Why Elasticsearch + MySQL?
"MySQL provides ACID compliance for transactional data, while Elasticsearch offers high-performance full-text search across multiple fields and entities."

### B. Why JWT Authentication?
"JWT tokens are stateless, scalable, and include user context (tenant, roles) eliminating additional database lookups on each request."

### C. Why Multi-Tenant at Database Level?
"Tenant ID on every table ensures data isolation is enforced by the database itself, preventing accidental data leakage even if application logic fails."

### D. Why Repository Pattern?
"Separates data access logic from business logic, making the codebase maintainable and testable."

### E. Why REST API?
"RESTful design allows frontend flexibility (web, mobile, desktop) and follows industry standards for API design."

---

## Quick Screenshot Checklist

☐ File/package structure
☐ Database ER diagram
☐ Postman API tests
☐ Elasticsearch indices
☐ JWT token (decoded)
☐ Frontend login
☐ Frontend search results
☐ application.properties
☐ Startup logs
☐ Class diagram (draw.io or similar)
☐ Architecture diagram (draw.io)

