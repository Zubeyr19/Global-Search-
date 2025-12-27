@echo off
REM ================================================================================
REM Performance Test Runner Script
REM Global Search System - 7th Semester Project
REM ================================================================================

echo ================================================================================
echo PERFORMANCE TESTING SUITE - Global Search System
echo ================================================================================
echo.

REM Set project directory
set PROJECT_DIR=C:\Users\Zubeyr\IdeaProjects\Global-Search-
set MAVEN=C:\Users\Zubeyr\apache-maven-3.9.11\bin\mvn.cmd
set JMETER=C:\Tools\apache-jmeter-5.6.2\bin\jmeter.bat

echo Project Directory: %PROJECT_DIR%
echo.

:MENU
echo ================================================================================
echo SELECT TEST TO RUN:
echo ================================================================================
echo 1. Run Indexing Latency Tests (U14)
echo 2. Run JMeter Load Test - GUI Mode
echo 3. Run JMeter Load Test - Command Line Mode
echo 4. Check Performance Metrics (API)
echo 5. Run All Tests
echo 6. View Performance Test Guide
echo 0. Exit
echo ================================================================================
echo.

set /p choice="Enter your choice (0-6): "

if "%choice%"=="1" goto INDEXING_TEST
if "%choice%"=="2" goto JMETER_GUI
if "%choice%"=="3" goto JMETER_CLI
if "%choice%"=="4" goto CHECK_METRICS
if "%choice%"=="5" goto RUN_ALL
if "%choice%"=="6" goto VIEW_GUIDE
if "%choice%"=="0" goto END
goto MENU

:INDEXING_TEST
echo.
echo ================================================================================
echo Running Indexing Latency Tests...
echo ================================================================================
echo.
cd /d "%PROJECT_DIR%"
"%MAVEN%" test -Dtest=IndexingLatencyTest
echo.
echo Tests completed! Press any key to return to menu...
pause >nul
goto MENU

:JMETER_GUI
echo.
echo ================================================================================
echo Starting JMeter in GUI Mode...
echo ================================================================================
echo.
echo IMPORTANT: Update JWT_TOKEN in User Defined Variables before running test!
echo.
echo Steps:
echo 1. Get JWT token by logging in: POST http://localhost:8080/api/auth/login
echo 2. In JMeter, update JWT_TOKEN variable
echo 3. Click green "Play" button to start test
echo 4. View results in Summary Report and Aggregate Report
echo.
pause
"%JMETER%" -t "%PROJECT_DIR%\performance-tests\jmeter\Global-Search-Load-Test.jmx"
goto MENU

:JMETER_CLI
echo.
echo ================================================================================
echo Running JMeter Load Test (Command Line Mode)...
echo ================================================================================
echo.
echo WARNING: Ensure JWT_TOKEN is updated in the .jmx file!
echo.
set /p continue="Continue? (y/n): "
if /i not "%continue%"=="y" goto MENU

cd /d "%PROJECT_DIR%"
mkdir performance-tests\results 2>nul

"%JMETER%" -n ^
  -t "%PROJECT_DIR%\performance-tests\jmeter\Global-Search-Load-Test.jmx" ^
  -l "%PROJECT_DIR%\performance-tests\results\results.jtl" ^
  -e ^
  -o "%PROJECT_DIR%\performance-tests\results\html-report"

echo.
echo ================================================================================
echo Test completed!
echo HTML Report: performance-tests\results\html-report\index.html
echo ================================================================================
echo.
echo Press any key to return to menu...
pause >nul
goto MENU

:CHECK_METRICS
echo.
echo ================================================================================
echo Checking Performance Metrics via API...
echo ================================================================================
echo.
echo Enter your JWT token (get from login endpoint):
set /p token="JWT Token: "
echo.

echo 1. Overall Performance Stats:
curl -X GET http://localhost:8080/api/performance/stats ^
  -H "Authorization: Bearer %token%"
echo.
echo.

echo 2. SLA Compliance:
curl -X GET http://localhost:8080/api/performance/sla-compliance ^
  -H "Authorization: Bearer %token%"
echo.
echo.

echo 3. Latency Distribution:
curl -X GET http://localhost:8080/api/performance/distribution ^
  -H "Authorization: Bearer %token%"
echo.
echo.

echo 4. Recent Slow Queries:
curl -X GET "http://localhost:8080/api/performance/slow-queries?limit=10" ^
  -H "Authorization: Bearer %token%"
echo.
echo.

echo Press any key to return to menu...
pause >nul
goto MENU

:RUN_ALL
echo.
echo ================================================================================
echo Running All Performance Tests...
echo ================================================================================
echo.

echo Step 1: Indexing Latency Tests
echo ----------------------------------------
cd /d "%PROJECT_DIR%"
"%MAVEN%" test -Dtest=IndexingLatencyTest
echo.

echo Step 2: Get JWT Token for Load Tests
echo ----------------------------------------
echo Please login to get JWT token:
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"superadmin\",\"password\":\"admin123\"}"
echo.
echo.
echo Copy the token from above and update it in the JMeter test plan.
pause

echo Step 3: JMeter Load Test
echo ----------------------------------------
"%JMETER%" -n ^
  -t "%PROJECT_DIR%\performance-tests\jmeter\Global-Search-Load-Test.jmx" ^
  -l "%PROJECT_DIR%\performance-tests\results\results.jtl" ^
  -e ^
  -o "%PROJECT_DIR%\performance-tests\results\html-report"

echo.
echo ================================================================================
echo All tests completed!
echo ================================================================================
echo.
echo Results:
echo - Indexing Latency: Check console output above
echo - Load Test Report: performance-tests\results\html-report\index.html
echo.
echo Press any key to return to menu...
pause >nul
goto MENU

:VIEW_GUIDE
echo.
echo ================================================================================
echo Opening Performance Testing Guide...
echo ================================================================================
start "" "%PROJECT_DIR%\performance-tests\PERFORMANCE_TESTING_GUIDE.md"
goto MENU

:END
echo.
echo ================================================================================
echo Exiting Performance Test Runner
echo ================================================================================
exit /b 0
