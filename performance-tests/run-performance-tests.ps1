# ================================================================================
# Performance Test Runner Script (PowerShell)
# Global Search System - 7th Semester Project
# ================================================================================

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "PERFORMANCE TESTING SUITE - Global Search System" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

# Set project directory
$ProjectDir = "C:\Users\Zubeyr\IdeaProjects\Global-Search-"
$Maven = "C:\Users\Zubeyr\apache-maven-3.9.11\bin\mvn.cmd"
$JMeter = "C:\Tools\apache-jmeter-5.6.2\bin\jmeter.bat"

Write-Host "Project Directory: $ProjectDir" -ForegroundColor Green
Write-Host ""

function Show-Menu {
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "SELECT TEST TO RUN:" -ForegroundColor Cyan
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "1. Run Indexing Latency Tests (U14)" -ForegroundColor White
    Write-Host "2. Run JMeter Load Test - GUI Mode" -ForegroundColor White
    Write-Host "3. Run JMeter Load Test - Command Line Mode" -ForegroundColor White
    Write-Host "4. Check Performance Metrics (API)" -ForegroundColor White
    Write-Host "5. Run All Tests" -ForegroundColor White
    Write-Host "6. View Performance Test Guide" -ForegroundColor White
    Write-Host "0. Exit" -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
}

function Test-IndexingLatency {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Running Indexing Latency Tests..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""

    Set-Location -Path $ProjectDir
    & $Maven test -Dtest=IndexingLatencyTest

    Write-Host ""
    Write-Host "Tests completed! Press any key to return to menu..." -ForegroundColor Green
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

function Start-JMeterGUI {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Starting JMeter in GUI Mode..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "IMPORTANT: Update JWT_TOKEN in User Defined Variables before running test!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Steps:" -ForegroundColor Yellow
    Write-Host "1. Get JWT token by logging in: POST http://localhost:8080/api/auth/login"
    Write-Host "2. In JMeter, update JWT_TOKEN variable"
    Write-Host "3. Click green 'Play' button to start test"
    Write-Host "4. View results in Summary Report and Aggregate Report"
    Write-Host ""

    Read-Host "Press Enter to continue"

    $TestPlan = Join-Path $ProjectDir "performance-tests\jmeter\Global-Search-Load-Test.jmx"
    & $JMeter -t $TestPlan
}

function Start-JMeterCLI {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Running JMeter Load Test (Command Line Mode)..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "WARNING: Ensure JWT_TOKEN is updated in the .jmx file!" -ForegroundColor Red
    Write-Host ""

    $continue = Read-Host "Continue? (y/n)"
    if ($continue -ne "y") {
        return
    }

    Set-Location -Path $ProjectDir

    # Create results directory
    $ResultsDir = Join-Path $ProjectDir "performance-tests\results"
    New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null

    $TestPlan = Join-Path $ProjectDir "performance-tests\jmeter\Global-Search-Load-Test.jmx"
    $ResultsFile = Join-Path $ResultsDir "results.jtl"
    $HtmlReport = Join-Path $ResultsDir "html-report"

    & $JMeter -n -t $TestPlan -l $ResultsFile -e -o $HtmlReport

    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Test completed!" -ForegroundColor Green
    Write-Host "HTML Report: performance-tests\results\html-report\index.html" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Press any key to return to menu..." -ForegroundColor Green
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

function Get-PerformanceMetrics {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Checking Performance Metrics via API..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""

    $token = Read-Host "Enter your JWT token (get from login endpoint)"
    Write-Host ""

    Write-Host "1. Overall Performance Stats:" -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $token"
    }

    try {
        $stats = Invoke-RestMethod -Uri "http://localhost:8080/api/performance/stats" -Headers $headers -Method Get
        $stats | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""

    Write-Host "2. SLA Compliance:" -ForegroundColor Yellow
    try {
        $sla = Invoke-RestMethod -Uri "http://localhost:8080/api/performance/sla-compliance" -Headers $headers -Method Get
        $sla | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""

    Write-Host "3. Latency Distribution:" -ForegroundColor Yellow
    try {
        $dist = Invoke-RestMethod -Uri "http://localhost:8080/api/performance/distribution" -Headers $headers -Method Get
        $dist | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""

    Write-Host "4. Recent Slow Queries:" -ForegroundColor Yellow
    try {
        $slow = Invoke-RestMethod -Uri "http://localhost:8080/api/performance/slow-queries?limit=10" -Headers $headers -Method Get
        $slow | ConvertTo-Json -Depth 10
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
    Write-Host ""

    Write-Host "Press any key to return to menu..." -ForegroundColor Green
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

function Start-AllTests {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Running All Performance Tests..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""

    Write-Host "Step 1: Indexing Latency Tests" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Set-Location -Path $ProjectDir
    & $Maven test -Dtest=IndexingLatencyTest
    Write-Host ""

    Write-Host "Step 2: Get JWT Token for Load Tests" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host "Please login to get JWT token:" -ForegroundColor Yellow

    $loginBody = @{
        username = "superadmin"
        password = "admin123"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
            -Method Post `
            -Body $loginBody `
            -ContentType "application/json"

        Write-Host "Login successful!" -ForegroundColor Green
        $response | ConvertTo-Json
    } catch {
        Write-Host "Login failed: $_" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Copy the token from above and update it in the JMeter test plan." -ForegroundColor Yellow
    Read-Host "Press Enter to continue"

    Write-Host "Step 3: JMeter Load Test" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow

    $ResultsDir = Join-Path $ProjectDir "performance-tests\results"
    New-Item -ItemType Directory -Force -Path $ResultsDir | Out-Null

    $TestPlan = Join-Path $ProjectDir "performance-tests\jmeter\Global-Search-Load-Test.jmx"
    $ResultsFile = Join-Path $ResultsDir "results.jtl"
    $HtmlReport = Join-Path $ResultsDir "html-report"

    & $JMeter -n -t $TestPlan -l $ResultsFile -e -o $HtmlReport

    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "All tests completed!" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Results:" -ForegroundColor Green
    Write-Host "- Indexing Latency: Check console output above" -ForegroundColor White
    Write-Host "- Load Test Report: performance-tests\results\html-report\index.html" -ForegroundColor White
    Write-Host ""
    Write-Host "Press any key to return to menu..." -ForegroundColor Green
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

function Show-Guide {
    Write-Host ""
    Write-Host "================================================================================" -ForegroundColor Cyan
    Write-Host "Opening Performance Testing Guide..." -ForegroundColor Yellow
    Write-Host "================================================================================" -ForegroundColor Cyan

    $GuidePath = Join-Path $ProjectDir "performance-tests\PERFORMANCE_TESTING_GUIDE.md"
    Start-Process $GuidePath
}

# Main loop
do {
    Clear-Host
    Show-Menu

    $choice = Read-Host "Enter your choice (0-6)"

    switch ($choice) {
        "1" { Test-IndexingLatency }
        "2" { Start-JMeterGUI }
        "3" { Start-JMeterCLI }
        "4" { Get-PerformanceMetrics }
        "5" { Start-AllTests }
        "6" { Show-Guide }
        "0" {
            Write-Host ""
            Write-Host "================================================================================" -ForegroundColor Cyan
            Write-Host "Exiting Performance Test Runner" -ForegroundColor Yellow
            Write-Host "================================================================================" -ForegroundColor Cyan
            exit
        }
        default {
            Write-Host "Invalid choice. Please try again." -ForegroundColor Red
            Start-Sleep -Seconds 2
        }
    }
} while ($true)
