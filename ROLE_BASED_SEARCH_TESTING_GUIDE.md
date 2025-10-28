# Role-Based Search Testing Guide

## Overview
This guide shows you how to test that the Global Search application properly enforces:
- **Multi-tenant isolation** - Users can only see their tenant's data
- **Role-based access control** - Different roles have different permissions
- **Cross-tenant access** - Only SUPER_ADMIN can see all tenants' data

---

## Test Data Setup

### Available Users
| Username | Password | Role | Tenant ID | Company ID |
|----------|----------|------|-----------|------------|
| superadmin | admin123 | SUPER_ADMIN | SYSTEM | null |
| admin_logistics | password123 | TENANT_ADMIN | TENANT_LOGISTICS | 1 |
| manager_chicago | password123 | MANAGER | TENANT_LOGISTICS | 1 |

### Data Distribution
- **TENANT_LOGISTICS**:
  - Company: Global Logistics Inc
  - 2 Locations
  - 2 Zones
  - 4 Sensors

- **TENANT_MANUFACTURING**:
  - Company: TechManufacturing Corp
  - Data exists but no users created yet

---

## Test Scenarios

### Test 1: SUPER_ADMIN Cross-Tenant Access ✅
**Expected**: SUPER_ADMIN should see data from ALL tenants

```bash
# Step 1: Login as superadmin
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}' \
  | grep -oP '"accessToken":"\K[^"]+'

# Save the token as SUPER_TOKEN
SUPER_TOKEN="<paste_token_here>"

# Step 2: Search for "logistics"
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $SUPER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"logistics","page":0,"size":20}' | jq .

# Step 3: Search for "manufacturing"
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $SUPER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"manufacturing","page":0,"size":20}' | jq .

# EXPECTED RESULT: Should return results from BOTH tenants
# - Should see "Global Logistics Inc" (TENANT_LOGISTICS)
# - Should see "TechManufacturing Corp" (TENANT_MANUFACTURING)
```

---

### Test 2: TENANT_ADMIN Isolation ✅
**Expected**: TENANT_ADMIN should ONLY see their tenant's data

```bash
# Step 1: Login as admin_logistics
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_logistics","password":"password123"}' \
  | grep -oP '"accessToken":"\K[^"]+'

# Save the token as TENANT_TOKEN
TENANT_TOKEN="<paste_token_here>"

# Step 2: Search for "logistics" (their own tenant)
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"logistics","page":0,"size":20}' | jq .

# Step 3: Try to search for "manufacturing" (different tenant)
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"manufacturing","page":0,"size":20}' | jq .

# EXPECTED RESULT:
# - Search for "logistics": Should return "Global Logistics Inc" data
# - Search for "manufacturing": Should return EMPTY or NO results
#   (because manufacturing belongs to TENANT_MANUFACTURING)
```

---

### Test 3: MANAGER Role Isolation ✅
**Expected**: MANAGER has same tenant restrictions as TENANT_ADMIN

```bash
# Step 1: Login as manager_chicago
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"manager_chicago","password":"password123"}' \
  | grep -oP '"accessToken":"\K[^"]+'

# Save the token as MANAGER_TOKEN
MANAGER_TOKEN="<paste_token_here>"

# Step 2: Search for their tenant's data
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"sensor","page":0,"size":20}' | jq .

# Step 3: Search for specific sensor by name
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $MANAGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"Temperature","page":0,"size":20}' | jq .

# EXPECTED RESULT:
# - Should ONLY see sensors from TENANT_LOGISTICS
# - Should NOT see any data from TENANT_MANUFACTURING
# - Total results should match their tenant's data count
```

---

### Test 4: Verify Tenant Filtering in Results ✅

```bash
# As superadmin, search and check all results have correct tenantId
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $SUPER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"company","page":0,"size":20}' | jq '.results[] | {entityType, entityName, tenantId}'

# EXPECTED RESULT:
# {
#   "entityType": "COMPANY",
#   "entityName": "Global Logistics Inc",
#   "tenantId": "TENANT_LOGISTICS"
# },
# {
#   "entityType": "COMPANY",
#   "entityName": "TechManufacturing Corp",
#   "tenantId": "TENANT_MANUFACTURING"
# }
```

---

### Test 5: Search by Entity Type with Tenant Restriction

```bash
# As admin_logistics, search for companies
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"","entityTypes":["COMPANY"],"page":0,"size":20}' | jq .

# EXPECTED RESULT:
# - Should return ONLY 1 company: "Global Logistics Inc"
# - Should NOT return "TechManufacturing Corp"

# As superadmin, search for companies
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $SUPER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"","entityTypes":["COMPANY"],"page":0,"size":20}' | jq .

# EXPECTED RESULT:
# - Should return BOTH companies:
#   1. "Global Logistics Inc" (TENANT_LOGISTICS)
#   2. "TechManufacturing Corp" (TENANT_MANUFACTURING)
```

---

## Validation Checklist

### ✅ Multi-Tenant Isolation Tests
- [ ] TENANT_ADMIN can only see their tenant's companies
- [ ] TENANT_ADMIN can only see their tenant's locations
- [ ] TENANT_ADMIN can only see their tenant's sensors
- [ ] TENANT_ADMIN cannot see other tenants' data
- [ ] MANAGER has same restrictions as TENANT_ADMIN
- [ ] Search results never leak data between tenants

### ✅ SUPER_ADMIN Privileges Tests
- [ ] SUPER_ADMIN can see all tenants' data
- [ ] SUPER_ADMIN can search across multiple tenants
- [ ] SUPER_ADMIN sees tenantId in all search results
- [ ] SUPER_ADMIN can use admin search endpoint

### ✅ Role-Based Access Tests
- [ ] Each role can only access their permitted endpoints
- [ ] Lower privilege roles cannot access admin endpoints
- [ ] JWT token contains correct roles
- [ ] Authentication fails with invalid credentials

---

## Common Issues & Solutions

### Issue 1: Seeing data from wrong tenant
**Symptom**: User sees data they shouldn't have access to
**Check**:
```bash
# Verify token has correct tenantId
echo $YOUR_TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```
**Fix**: Ensure `SearchService` properly filters by `user.getTenantId()`

### Issue 2: SUPER_ADMIN not seeing all data
**Symptom**: SUPER_ADMIN only sees one tenant's data
**Check**: Verify SUPER_ADMIN bypass logic in `SearchService`
**Fix**: SUPER_ADMIN should skip tenant filtering

### Issue 3: Empty search results
**Symptom**: Valid search returns no results
**Check**:
```bash
# Check Elasticsearch has data
curl -s "http://localhost:9200/companies/_count"
curl -s "http://localhost:9200/sensors/_count"
```
**Fix**: Run sync if needed: `POST /api/admin/elasticsearch/sync/all`

---

## Advanced Testing

### Test Fuzzy Search with Tenant Isolation
```bash
# Search with typo - should still respect tenant boundaries
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query":"logistcs",
    "enableFuzzySearch":true,
    "fuzzyMaxEdits":2,
    "page":0,
    "size":20
  }' | jq .
```

### Test Highlighting with Tenant Isolation
```bash
# Search with highlighting - results should still be filtered by tenant
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query":"temperature",
    "enableHighlighting":true,
    "page":0,
    "size":20
  }' | jq .
```

### Test Pagination with Tenant Data
```bash
# Page 1 - should only show tenant's data
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"","page":0,"size":2}' | jq '{totalResults, page, size}'

# Page 2 - should continue with tenant's data only
curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"","page":1,"size":2}' | jq '{totalResults, page, size}'
```

---

## Security Verification Script

Create a file `test_security.sh`:
```bash
#!/bin/bash

echo "=== Testing Multi-Tenant Isolation ==="

# Get tokens
SUPER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"admin123"}' | jq -r '.accessToken')

TENANT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin_logistics","password":"password123"}' | jq -r '.accessToken')

echo "✓ Tokens obtained"

# Test 1: SUPER_ADMIN sees all
SUPER_RESULTS=$(curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $SUPER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"company","page":0,"size":20}' | jq '.totalResults')

echo "SUPER_ADMIN sees $SUPER_RESULTS companies"

# Test 2: TENANT_ADMIN sees only theirs
TENANT_RESULTS=$(curl -s -X POST "http://localhost:8080/api/search" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"company","page":0,"size":20}' | jq '.totalResults')

echo "TENANT_ADMIN sees $TENANT_RESULTS companies"

# Validation
if [ "$SUPER_RESULTS" -gt "$TENANT_RESULTS" ]; then
  echo "✅ PASS: Multi-tenant isolation working correctly"
else
  echo "❌ FAIL: Tenant isolation may be broken"
fi
```

---

## Expected Outcomes Summary

| User Role | Can See TENANT_LOGISTICS | Can See TENANT_MANUFACTURING | Can See All |
|-----------|--------------------------|------------------------------|-------------|
| SUPER_ADMIN | ✅ Yes | ✅ Yes | ✅ Yes |
| TENANT_ADMIN (Logistics) | ✅ Yes | ❌ No | ❌ No |
| MANAGER (Logistics) | ✅ Yes | ❌ No | ❌ No |

---

## Next Steps

1. Run each test scenario
2. Record actual results vs expected results
3. If tests fail, check:
   - JWT token payload (decode and verify tenantId)
   - SearchService tenant filtering logic
   - Elasticsearch query filters
   - Audit logs for access attempts

4. For production deployment:
   - Add automated integration tests
   - Add security audit logging
   - Monitor cross-tenant access attempts
   - Set up alerts for unauthorized access

---

**Last Updated**: October 23, 2025
**Application Version**: 1.0-SNAPSHOT
**Testing Status**: Ready for execution
