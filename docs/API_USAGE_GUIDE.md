# Global Search - API Usage Guide

## Table of Contents
1. [Getting Started](#getting-started)
2. [Authentication](#authentication)
3. [Search Endpoints](#search-endpoints)
4. [Admin Endpoints](#admin-endpoints)
5. [Policy Management](#policy-management)
6. [Export Endpoints](#export-endpoints)
7. [WebSocket Notifications](#websocket-notifications)
8. [Error Handling](#error-handling)
9. [Rate Limiting](#rate-limiting)
10. [Best Practices](#best-practices)

---

## Getting Started

**Base URL:** `http://localhost:8080`

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

All endpoints (except `/api/auth/login` and `/api/auth/register`) require authentication via JWT token in the `Authorization` header.

### Headers

```
Content-Type: application/json
Authorization: Bearer {your_jwt_token}
```

---

## Authentication

### 1. Login

**Endpoint:** `POST /api/auth/login`

**Description:** Authenticate user and receive JWT tokens

**Request:**
```json
{
  "username": "superadmin",
  "password": "admin123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "username": "superadmin",
  "email": "superadmin@globalsearch.com",
  "fullName": "Super Admin",
  "tenantId": "SYSTEM",
  "companyName": null,
  "roles": ["SUPER_ADMIN"]
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin",
    "password": "admin123"
  }'
```

### 2. Refresh Token

**Endpoint:** `POST /api/auth/refresh`

**Description:** Get new access token using refresh token

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### 3. Logout

**Endpoint:** `POST /api/auth/logout`

**Headers:** `Authorization: Bearer {token}`

**Response (200 OK):**
```json
{
  "message": "Successfully logged out"
}
```

---

## Search Endpoints

### 1. Global Search

**Endpoint:** `POST /api/search`

**Description:** Search across all entity types (filtered by user's tenant and role)

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "query": "temperature",
  "entityTypes": ["Sensor", "Location"],
  "page": 0,
  "size": 20,
  "sortBy": "createdAt",
  "sortDirection": "DESC",
  "enableFuzzySearch": true,
  "enableHighlighting": true,
  "filters": {
    "city": "Copenhagen",
    "country": "Denmark",
    "sensorType": "TEMPERATURE",
    "status": "ACTIVE"
  }
}
```

**Parameters:**
- `query` (required): Search term
- `entityTypes` (optional): Filter by entity types. Options: `Company`, `Location`, `Zone`, `Sensor`, `Report`, `Dashboard`
- `page` (optional, default: 0): Page number (0-indexed)
- `size` (optional, default: 20, max: 100): Results per page
- `sortBy` (optional): Field to sort by
- `sortDirection` (optional): `ASC` or `DESC`
- `enableFuzzySearch` (optional, default: false): Allow fuzzy matching
- `enableHighlighting` (optional, default: false): Highlight matching text
- `filters` (optional): Additional filters

**Response (200 OK):**
```json
{
  "results": [
    {
      "id": 1,
      "type": "Sensor",
      "name": "Temperature Sensor 01",
      "description": "Main warehouse temperature sensor",
      "location": "Warehouse A",
      "zone": "Zone 1",
      "status": "ACTIVE",
      "createdAt": "2025-10-22T14:26:58",
      "score": 0.94,
      "highlights": [
        "<em>Temperature</em> Sensor 01"
      ]
    },
    {
      "id": 2,
      "type": "Location",
      "name": "Temperature Controlled Zone",
      "city": "Copenhagen",
      "country": "Denmark",
      "score": 0.82
    }
  ],
  "totalResults": 8,
  "currentPage": 0,
  "totalPages": 1,
  "pageSize": 20,
  "searchDurationMs": 45
}
```

**cURL Example:**
```bash
TOKEN="your_jwt_token_here"

curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "temperature",
    "entityTypes": ["Sensor"],
    "page": 0,
    "size": 20,
    "enableFuzzySearch": true
  }'
```

### 2. Quick Search (Autocomplete)

**Endpoint:** `GET /api/search/quick?q={query}`

**Description:** Fast autocomplete suggestions

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "suggestions": [
    {
      "text": "Temperature Sensor 01",
      "type": "Sensor",
      "id": 1
    },
    {
      "text": "Temperature Controlled Zone",
      "type": "Location",
      "id": 5
    }
  ]
}
```

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/search/quick?q=temp" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Search by Entity Type

**Endpoint:** `POST /api/search/sensors` (replace `sensors` with `companies`, `locations`, etc.)

**Description:** Search within a specific entity type

**Request:**
```json
{
  "query": "temperature",
  "page": 0,
  "size": 20
}
```

**Response:** Similar to Global Search, but only returns specified entity type

---

## Admin Endpoints

### 1. Admin Global Search (Cross-Tenant)

**Endpoint:** `POST /api/search/admin`

**Description:** Search across ALL tenants (SUPER_ADMIN only)

**Required Role:** `SUPER_ADMIN`

**Request:**
```json
{
  "query": "sensor",
  "tenantIds": ["TENANT_A", "TENANT_B"],
  "entityTypes": ["Sensor"],
  "page": 0,
  "size": 50
}
```

**Response:** Similar to Global Search, includes data from multiple tenants

### 2. Admin Dashboard

**Endpoint:** `GET /api/admin/dashboard`

**Description:** Get system overview statistics

**Required Role:** `SUPER_ADMIN` or `TENANT_ADMIN`

**Response (200 OK):**
```json
{
  "totalUsers": 125,
  "totalCompanies": 15,
  "totalLocations": 48,
  "totalSensors": 1250,
  "activeUsers": 87,
  "systemHealth": "HEALTHY",
  "storageUsed": "245GB",
  "todaySearches": 1543,
  "averageResponseTime": "342ms"
}
```

### 3. User Management

#### List Users
**Endpoint:** `GET /api/admin/users?page=0&size=20`

**Required Role:** `SUPER_ADMIN` or `TENANT_ADMIN`

**Response:**
```json
{
  "users": [
    {
      "id": 1,
      "username": "john.doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "role": "MANAGER",
      "tenantId": "TENANT_A",
      "active": true,
      "createdAt": "2025-10-01T10:00:00"
    }
  ],
  "total": 125,
  "page": 0,
  "pages": 7
}
```

#### Create User
**Endpoint:** `POST /api/admin/users`

**Request:**
```json
{
  "username": "new.user",
  "password": "SecureP@ssw0rd",
  "email": "new.user@example.com",
  "fullName": "New User",
  "role": "VIEWER",
  "companyId": 5
}
```

#### Update User
**Endpoint:** `PUT /api/admin/users/{userId}`

#### Delete User
**Endpoint:** `DELETE /api/admin/users/{userId}`

---

## Policy Management

### 1. List Policies

**Endpoint:** `GET /api/policies?includeInactive=false`

**Required Role:** `SUPER_ADMIN` or `TENANT_ADMIN`

**Response (200 OK):**
```json
{
  "policies": [
    {
      "id": 1,
      "name": "Sensor Read Policy",
      "description": "Allow viewers to read sensors",
      "role": "VIEWER",
      "rules": "{\"entities\": [\"Sensor\"], \"actions\": [\"READ\"]}",
      "active": true,
      "createdAt": "2025-10-15T08:00:00",
      "updatedAt": "2025-10-15T08:00:00"
    }
  ]
}
```

### 2. Create Policy

**Endpoint:** `POST /api/policies`

**Request:**
```json
{
  "name": "Sensor Management Policy",
  "description": "Allow operators to manage sensors",
  "role": "OPERATOR",
  "rules": "{\"entities\": [\"Sensor\"], \"actions\": [\"READ\", \"UPDATE\"]}",
  "active": true
}
```

**Response (201 CREATED):**
```json
{
  "id": 2,
  "name": "Sensor Management Policy",
  "tenantId": "TENANT_A",
  ...
}
```

### 3. Update Policy

**Endpoint:** `PUT /api/policies/{policyId}`

### 4. Delete Policy

**Endpoint:** `DELETE /api/policies/{policyId}`

---

## Export Endpoints

### 1. Export Search Results

**Endpoint:** `GET /api/export/search?query={query}&format={format}`

**Parameters:**
- `query`: Search query
- `format`: `csv`, `pdf`, or `excel`

**Response:** File download

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/export/search?query=sensor&format=csv" \
  -H "Authorization: Bearer $TOKEN" \
  --output results.csv
```

### 2. Export Users

**Endpoint:** `GET /api/admin/export/users?format=csv`

**Required Role:** `SUPER_ADMIN` or `TENANT_ADMIN`

**Formats:** `csv`, `pdf`, `excel`

### 3. Export Audit Logs

**Endpoint:** `GET /api/admin/export/audit?startDate=2025-10-01&endDate=2025-10-28&format=pdf`

**Required Role:** `SUPER_ADMIN` or `TENANT_ADMIN`

---

## WebSocket Notifications

### Connection

**URL:** `ws://localhost:8080/ws`

**JavaScript Example:**
```javascript
// Using SockJS and STOMP
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // Subscribe to user notifications
    stompClient.subscribe('/topic/notifications/123', function(message) {
        const notification = JSON.parse(message.body);
        console.log('Notification:', notification);
    });
});
```

### Notification Types

**Search Complete:**
```json
{
  "type": "SEARCH_COMPLETE",
  "queryId": "abc123",
  "resultCount": 45,
  "duration": "342ms",
  "timestamp": "2025-10-28T10:30:00"
}
```

**Data Updated:**
```json
{
  "type": "DATA_UPDATED",
  "entityType": "Sensor",
  "entityId": 15,
  "action": "UPDATE",
  "timestamp": "2025-10-28T10:30:00"
}
```

---

## Error Handling

### Error Response Format

```json
{
  "timestamp": "2025-10-28T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid search query: Query cannot be empty",
  "path": "/api/search",
  "requestId": "abc-123-def"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 204 | No Content | Successful deletion |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

### Common Error Scenarios

#### 1. Unauthorized (401)
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**Solution:** Include valid JWT token in Authorization header

#### 2. Forbidden (403)
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions to access this resource"
}
```

**Solution:** Check user role requirements for endpoint

#### 3. Rate Limit (429)
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 60 seconds.",
  "retryAfter": 60
}
```

**Solution:** Reduce request frequency, wait for retry period

---

## Rate Limiting

**Current Limits:**
- 100 requests per minute per IP address
- Applies to all authenticated endpoints
- Excludes health check and static resources

**Headers:**
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1635424800
```

---

## Best Practices

### 1. Authentication
- Store JWT tokens securely (HttpOnly cookies or secure storage)
- Refresh tokens before they expire
- Never log or expose tokens
- Use HTTPS in production

### 2. Search Optimization
- Use pagination (max 100 results per page)
- Be specific with entity types to reduce search scope
- Use filters to narrow results
- Enable fuzzy search only when needed (impacts performance)
- Cache frequently used searches

### 3. Error Handling
- Always check HTTP status codes
- Implement exponential backoff for rate limit errors
- Log errors with request IDs for debugging
- Validate input before sending requests

### 4. Performance
- Batch operations when possible
- Use quick search for autocomplete (faster)
- Avoid searching in loops
- Monitor response times
- Use async/await for non-blocking calls

### 5. Security
- Never commit API tokens
- Rotate JWT secrets regularly
- Validate all user input
- Use environment variables for configuration
- Enable HTTPS in production

---

## Example Integration (JavaScript)

```javascript
class GlobalSearchClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl;
        this.token = null;
    }

    async login(username, password) {
        const response = await fetch(`${this.baseUrl}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await response.json();
        this.token = data.accessToken;
        return data;
    }

    async search(query, options = {}) {
        const response = await fetch(`${this.baseUrl}/api/search`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.token}`
            },
            body: JSON.stringify({ query, ...options })
        });
        return await response.json();
    }

    async quickSearch(query) {
        const response = await fetch(
            `${this.baseUrl}/api/search/quick?q=${encodeURIComponent(query)}`,
            {
                headers: { 'Authorization': `Bearer ${this.token}` }
            }
        );
        return await response.json();
    }
}

// Usage
const client = new GlobalSearchClient('http://localhost:8080');
await client.login('superadmin', 'admin123');
const results = await client.search('temperature', {
    entityTypes: ['Sensor'],
    page: 0,
    size: 20
});
console.log(results);
```

---

## API Testing Tools

### Postman Collection
Import the Swagger JSON for automatic collection generation:
1. Open Postman
2. Import → Link: `http://localhost:8080/v3/api-docs`
3. All endpoints will be available

### cURL Testing Script
```bash
#!/bin/bash

# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}' \
  | jq -r '.accessToken')

echo "Token: $TOKEN"

# Search
curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"sensor","page":0,"size":10}' \
  | jq .
```

---

## Support & Feedback

- **Swagger UI:** http://localhost:8080/swagger-ui.html (Interactive API docs)
- **Issues:** https://github.com/Zubeyr19/Global-Search-/issues
- **Documentation:** `/docs` folder
- **Architecture:** See `docs/ARCHITECTURE.md`

---

**Document Version:** 1.0
**Last Updated:** 2025-10-28
**Author:** Development Team
