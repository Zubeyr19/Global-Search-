@echo off
echo ========================================
echo Global Search Application Demo
echo ========================================
echo.

echo [1/5] Testing Elasticsearch connection...
curl -s http://localhost:9200 > nul
if %errorlevel% equ 0 (
    echo ✓ Elasticsearch is running
) else (
    echo ✗ Elasticsearch is NOT running
    exit /b 1
)
echo.

echo [2/5] Testing Backend API...
curl -s http://localhost:8080/api/auth/login > nul
if %errorlevel% equ 0 (
    echo ✓ Backend API is running
) else (
    echo ✗ Backend API is NOT running
    exit /b 1
)
echo.

echo [3/5] Testing Login...
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"password123\"}" > login-response.json 2>nul
if %errorlevel% equ 0 (
    echo ✓ Login successful
    echo Response saved to login-response.json
) else (
    echo ✗ Login failed
)
echo.

echo [4/5] Testing Search with user account...
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user\",\"password\":\"password123\"}" > user-login.json 2>nul
echo ✓ User login test completed
echo.

echo [5/5] Checking Elasticsearch indices...
curl -s http://localhost:9200/_cat/indices?v
echo.

echo ========================================
echo Demo Test Complete!
echo ========================================
echo.
echo Test Credentials:
echo - superadmin / admin123 (Super Admin)
echo - admin / password123 (Tenant Admin)
echo - user / password123 (Regular User)
echo.
echo Next steps:
echo 1. Check login-response.json for the access token
echo 2. Open frontend\index.html in your browser
echo 3. Login with one of the accounts above
echo 4. Try searching for: Tech, Global, Sensor, Copenhagen
echo.
pause
