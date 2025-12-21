@echo off
REM Quick start script for Docker Compose

echo ====================================
echo Global Search - Docker Quick Start
echo ====================================
echo.

REM Check if Docker is running
docker info > nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    echo.
    pause
    exit /b 1
)

echo Docker is running...
echo.

REM Check if user wants to start or stop
if "%1"=="stop" goto stop
if "%1"=="down" goto down
if "%1"=="logs" goto logs
if "%1"=="clean" goto clean

:start
echo Starting all services (MySQL, Elasticsearch, Spring Boot)...
echo This may take 60-90 seconds on first run...
echo.
docker-compose up -d
echo.
echo ====================================
echo Services started!
echo ====================================
echo.
echo Application: http://localhost:8080
echo Elasticsearch: http://localhost:9200
echo MySQL: localhost:3306
echo.
echo Run 'docker-start.bat logs' to view logs
echo Run 'docker-start.bat stop' to stop services
echo.
goto health_check

:stop
echo Stopping all services...
docker-compose stop
echo Services stopped.
goto end

:down
echo Stopping and removing all containers...
docker-compose down
echo All containers removed.
goto end

:logs
echo Showing application logs (Ctrl+C to exit)...
docker-compose logs -f app
goto end

:clean
echo WARNING: This will delete all data!
set /p confirm="Are you sure? (yes/no): "
if /i "%confirm%"=="yes" (
    echo Cleaning up...
    docker-compose down -v
    echo Cleanup complete.
) else (
    echo Cleanup cancelled.
)
goto end

:health_check
echo Waiting for services to be healthy...
timeout /t 5 /nobreak > nul
docker-compose ps
echo.
echo Run 'curl http://localhost:8080/actuator/health' to check application health
goto end

:end
pause
