@echo off
REM ===========================================================================
REM PERFORMANCE FIX APPLICATION SCRIPT
REM ===========================================================================
REM This script applies all performance optimizations to the Global Search system
REM ===========================================================================

echo.
echo ========================================================================
echo GLOBAL SEARCH - PERFORMANCE OPTIMIZATION SCRIPT
echo ========================================================================
echo.
echo This script will:
echo   1. Apply database indexes to MySQL
echo   2. Verify Elasticsearch is running
echo   3. Restart the application
echo   4. Verify performance improvements
echo.
echo WARNING: This will restart your application!
echo.
pause

REM ===========================================================================
REM STEP 1: Check if Elasticsearch is running
REM ===========================================================================
echo.
echo [STEP 1/5] Checking Elasticsearch status...
curl -s http://localhost:9200 >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Elasticsearch is not running on port 9200!
    echo Please start Elasticsearch first:
    echo   cd C:\elasticsearch-8.13.2
    echo   bin\elasticsearch.bat
    pause
    exit /b 1
)
echo [OK] Elasticsearch is running

REM ===========================================================================
REM STEP 2: Apply database indexes
REM ===========================================================================
echo.
echo [STEP 2/5] Applying database indexes...
echo Enter MySQL root password when prompted:
mysql -u root -p global_search_db < database-indexes.sql
if %errorlevel% neq 0 (
    echo [ERROR] Failed to apply database indexes!
    pause
    exit /b 1
)
echo [OK] Database indexes applied successfully

REM ===========================================================================
REM STEP 3: Kill any running instances on port 8080
REM ===========================================================================
echo.
echo [STEP 3/5] Stopping existing application instances...
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8080" ^| find "LISTENING"') do (
    echo Killing process %%a on port 8080...
    taskkill /F /PID %%a >nul 2>&1
)
timeout /t 3 /nobreak >nul
echo [OK] Port 8080 is now available

REM ===========================================================================
REM STEP 4: Start the application
REM ===========================================================================
echo.
echo [STEP 4/5] Starting Global Search application...
echo.
echo NOTE: Application will start in a new window
echo Watch the logs for:
echo   - "Elasticsearch synchronization completed"
echo   - "GlobalSearchHikariCP - Start completed"
echo   - "Started GlobalSearchApplication"
echo.
start "Global Search" cmd /k "cd /d C:\Users\Zubeyr\IdeaProjects\Global-Search- && C:\Users\Zubeyr\apache-maven-3.9.11\bin\mvn.cmd spring-boot:run"

echo Waiting for application to start (30 seconds)...
timeout /t 30 /nobreak >nul

REM ===========================================================================
REM STEP 5: Verify performance
REM ===========================================================================
echo.
echo [STEP 5/5] Verifying performance improvements...
echo.

echo Checking Elasticsearch indices...
echo.
echo Companies index:
curl -s http://localhost:9200/companies/_count
echo.
echo.
echo Sensors index:
curl -s http://localhost:9200/sensors/_count
echo.
echo.
echo Locations index:
curl -s http://localhost:9200/locations/_count
echo.
echo.

echo.
echo ========================================================================
echo PERFORMANCE OPTIMIZATION COMPLETE!
echo ========================================================================
echo.
echo Next Steps:
echo   1. Check the application log window for any errors
echo   2. Test login: http://localhost:8080/swagger-ui/index.html
echo   3. Perform a search and check response time in logs
echo   4. Run performance tests to validate ^<1s latency
echo.
echo Expected Improvements:
echo   - Simple queries: ^<10ms
echo   - Complex searches: ^<100ms
echo   - Full-text searches: ^<500ms
echo   - Connection pooling: 20 connections ready
echo   - Elasticsearch: All indices populated
echo.
echo Documentation:
echo   - See PERFORMANCE_OPTIMIZATIONS.md for details
echo   - Database indexes: database-indexes.sql
echo.
pause
