#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Run Spring Boot backend locally with Maven using the same environment as Docker container.

.DESCRIPTION
    This script:
    1. Loads environment variables from .env file (source of truth)
    2. Adjusts URLs for local execution (localhost instead of host.docker.internal)
    3. Starts Spring Boot with Maven Wrapper in the background
    4. Provides commands to check logs and stop the process

.EXAMPLE
    .\run-local.ps1
    # Starts backend in background

.EXAMPLE
    .\run-local.ps1 -Foreground
    # Starts backend in foreground (see logs directly)

.EXAMPLE
    .\run-local.ps1 -Stop
    # Stops the running backend process

.NOTES
    Requirements:
    - Java 21 (JDK) installed
    - .env file in backend directory
    - SQL Server container running on localhost:1433
    - Keycloak container running on localhost:9090
#>

param(
    [Parameter(Mandatory=$false)]
    [switch]$Foreground,
    
    [Parameter(Mandatory=$false)]
    [switch]$Stop,
    
    [Parameter(Mandatory=$false)]
    [switch]$Logs
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$BackendDir = $ScriptDir
$EnvFile = Join-Path $BackendDir ".env"
$PidFile = Join-Path $BackendDir ".backend-local.pid"
$LogFile = Join-Path $BackendDir "backend-local.log"

# Color output functions
function Write-InfoMsg {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Write-SuccessMsg {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-ErrorMsg {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# Function to stop running backend
function Stop-Backend {
    if (Test-Path $PidFile) {
        $BackendPid = Get-Content $PidFile
        Write-InfoMsg "Stopping backend process (PID: $BackendPid)..."
        
        try {
            Stop-Process -Id $BackendPid -Force -ErrorAction SilentlyContinue
            Remove-Item $PidFile -ErrorAction SilentlyContinue
            Write-SuccessMsg "Backend stopped"
        } catch {
            Write-ErrorMsg "Failed to stop process: $_"
        }
    } else {
        Write-InfoMsg "No running backend process found"
    }
}

# Function to show logs
function Show-Logs {
    if (Test-Path $LogFile) {
        Write-InfoMsg "Showing backend logs (Ctrl+C to exit)..."
        Get-Content $LogFile -Wait
    } else {
        Write-ErrorMsg "Log file not found: $LogFile"
    }
}

# Handle stop and logs flags
if ($Stop) {
    Stop-Backend
    exit 0
}

if ($Logs) {
    Show-Logs
    exit 0
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Yellow
Write-Host "RAP Backend - Local Development with Maven" -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Yellow
Write-Host ""

# Check if Java is installed
Write-InfoMsg "Checking Java installation..."
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-SuccessMsg "Java found: $javaVersion"
} catch {
    Write-ErrorMsg "Java is not installed or not in PATH"
    Write-InfoMsg "Install Java 21: winget install Microsoft.OpenJDK.21"
    exit 1
}

# Check if .env file exists
if (-not (Test-Path $EnvFile)) {
    Write-ErrorMsg ".env file not found: $EnvFile"
    Write-InfoMsg "Run './dev.ps1 Setup' to create .env from .env.example"
    exit 1
}

Write-InfoMsg "Loading environment variables from .env..."

# Load all environment variables from .env file
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        Set-Item -Path "env:$key" -Value $value
    }
}

Write-SuccessMsg "Environment variables loaded from .env"

# Override Docker-specific URLs for local execution
Write-InfoMsg "Adjusting URLs for local execution..."

# SQL Server connection (local: localhost, container: database or host.docker.internal)
$env:AZURE_SQL_CONNECTIONSTRING = "jdbc:sqlserver://localhost:1433;databaseName=raptordb;user=$env:DB_USERNAME;password=$env:DB_PASSWORD;encrypt=true;trustServerCertificate=true"

# Keycloak URLs (local: localhost, container: host.docker.internal)
$env:OIDC_TOKEN_URI = "http://localhost:9090/realms/raptor/protocol/openid-connect/token"
$env:OIDC_USER_INFO_URI = "http://localhost:9090/realms/raptor/protocol/openid-connect/userinfo"
$env:OIDC_JWK_SET_URI = "http://localhost:9090/realms/raptor/protocol/openid-connect/certs"

Write-SuccessMsg "URLs configured for local execution"

# Display key configuration
Write-Host ""
Write-Host "Configuration Summary:" -ForegroundColor Cyan
Write-Host "  Database:     localhost:1433 (raptordb)" -ForegroundColor White
Write-Host "  Keycloak:     localhost:9090 (realm: raptor)" -ForegroundColor White
Write-Host "  Frontend URL: $env:FRONTEND_URL" -ForegroundColor White
Write-Host "  OIDC Client:  $env:OIDC_CLIENT_ID" -ForegroundColor White
Write-Host ""

# Stop any existing backend process
if (Test-Path $PidFile) {
    Write-InfoMsg "Stopping existing backend process..."
    Stop-Backend
}

# Change to backend directory
Push-Location $BackendDir

try {
    if ($Foreground) {
        # Run in foreground
        Write-InfoMsg "Starting backend in foreground mode..."
        Write-InfoMsg "Press Ctrl+C to stop"
        Write-Host ""
        
        & .\mvnw.cmd spring-boot:run
    } else {
        # Run in background
        Write-InfoMsg "Starting backend in background..."
        
        # Start Maven process in background and capture output using pwsh
        $process = Start-Process -FilePath "pwsh.exe" `
            -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", "Set-Location '$BackendDir'; .\mvnw.cmd spring-boot:run *>&1 | Tee-Object -FilePath '$LogFile'" `
            -WindowStyle Hidden `
            -PassThru
        
        # Save PID
        $process.Id | Out-File $PidFile
        
        Write-SuccessMsg "Backend started in background (PID: $($process.Id))"
        Write-InfoMsg "Log file: $LogFile"
        Write-Host ""
        Write-Host "Useful commands:" -ForegroundColor Cyan
        Write-Host "  View logs:  .\run-local.ps1 -Logs" -ForegroundColor White
        Write-Host "  Stop:       .\run-local.ps1 -Stop" -ForegroundColor White
        Write-Host "  Or use:     Get-Content backend-local.log -Wait" -ForegroundColor White
        Write-Host ""
        
        # Wait for backend to start
        Write-InfoMsg "Waiting for backend to start (15 seconds)..."
        Start-Sleep -Seconds 15
        
        # Check if process is still running
        if (Get-Process -Id $process.Id -ErrorAction SilentlyContinue) {
            # Test health endpoint
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 5 -ErrorAction Stop
                $health = $response.Content | ConvertFrom-Json
                
                if ($health.status -eq "UP") {
                    Write-SuccessMsg "Backend is healthy and ready!"
                    Write-InfoMsg "Health check: http://localhost:8080/actuator/health"
                } else {
                    Write-ErrorMsg "Backend is running but not healthy: $($health.status)"
                    Write-InfoMsg "Check logs: .\run-local.ps1 -Logs"
                }
            } catch {
                Write-ErrorMsg "Backend is running but health check failed"
                Write-InfoMsg "It may still be starting up. Check logs: .\run-local.ps1 -Logs"
            }
        } else {
            Write-ErrorMsg "Backend process stopped unexpectedly"
            Write-InfoMsg "Check logs: Get-Content $LogFile"
            Remove-Item $PidFile -ErrorAction SilentlyContinue
            exit 1
        }
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "Backend is running!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Test URLs:" -ForegroundColor Cyan
Write-Host "  Health:     http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "  Auth Login: http://localhost:8080/auth/login" -ForegroundColor White
Write-Host "  API:        http://localhost:8080/api/applications" -ForegroundColor White
Write-Host ""
