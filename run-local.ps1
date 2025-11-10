#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Run Spring Boot backend locally with Maven using the same environment as Docker container.

.DESCRIPTION
    This script provides commands to run the backend locally via Maven (not Docker).
    Supports both foreground mode (with hot reload) and background mode.
    
    Commands:
    - Start (default): Run backend in foreground with hot reload
    - Background: Run backend in background mode
    - Stop: Stop the running backend process
    - Logs: Show live logs from background process
    - Help: Show detailed help information

.EXAMPLE
    .\run-local.ps1
    # Starts backend in foreground (default) - supports hot reload

.EXAMPLE
    .\run-local.ps1 Background
    # Starts backend in background mode

.EXAMPLE
    .\run-local.ps1 Stop
    # Stops the running backend process

.EXAMPLE
    .\run-local.ps1 Logs
    # Shows live logs from background process

.EXAMPLE
    .\run-local.ps1 Help
    # Shows detailed help information

.NOTES
    Requirements:
    - Java 21 (JDK) installed
    - .env file in backend directory
    - SQL Server container running on localhost:1433
    - Keycloak container running on localhost:9090
    
    Hot Code Deployment:
    - Start (foreground): Changes are auto-detected and reloaded (via Spring Boot DevTools)
    - Background mode: Changes require manual restart
    - In VS Code Agent mode: Changes are NOT deployed until you accept them
#>

param(
    [Parameter(Position=0)]
    [string]$Command = "Start"
)

$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot
$BackendDir = $ScriptDir
$EnvFile = Join-Path $BackendDir ".env"
$PidFile = Join-Path $BackendDir ".backend-local.pid"
$LogFile = Join-Path $BackendDir "backend-local.log"

# Default port (will be overridden from .env file)
$AppPort = 8080

# Function to load environment variables and get port
function Get-AppPort {
    $port = 8080  # Default
    
    if (Test-Path $EnvFile) {
        $content = Get-Content $EnvFile
        foreach ($line in $content) {
            if ($line -match '^APP_PORT=(.*)$') {
                $port = [int]$matches[1].Trim()
                break
            }
        }
    }
    
    return $port
}

# Function to show detailed help
function Show-Help {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host "RAP Backend - Local Maven Development Script" -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "USAGE:" -ForegroundColor Cyan
    Write-Host "  .\run-local.ps1 [COMMAND]" -ForegroundColor White
    Write-Host ""
    Write-Host "COMMANDS:" -ForegroundColor Cyan
    Write-Host "  Start (default) " -NoNewline -ForegroundColor White
    Write-Host "Run backend in FOREGROUND mode" -ForegroundColor Gray
    Write-Host "                  " -NoNewline
    Write-Host "• Hot reload enabled - code changes auto-detect" -ForegroundColor DarkGray
    Write-Host "                  " -NoNewline
    Write-Host "• Press Ctrl+C to stop" -ForegroundColor DarkGray
    Write-Host "                  " -NoNewline
    Write-Host "• Best for active development" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  Background      " -NoNewline -ForegroundColor White
    Write-Host "Run backend in BACKGROUND mode" -ForegroundColor Gray
    Write-Host "                  " -NoNewline
    Write-Host "• Runs as hidden process" -ForegroundColor DarkGray
    Write-Host "                  " -NoNewline
    Write-Host "• Logs to backend-local.log" -ForegroundColor DarkGray
    Write-Host "                  " -NoNewline
    Write-Host "• Changes require manual restart" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  Stop            " -NoNewline -ForegroundColor White
    Write-Host "Stop the running backend process" -ForegroundColor Gray
    Write-Host "                  " -NoNewline
    Write-Host "• Terminates background process if running" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  Logs            " -NoNewline -ForegroundColor White
    Write-Host "Show live logs from background process" -ForegroundColor Gray
    Write-Host "                  " -NoNewline
    Write-Host "• Follows log file in real-time" -ForegroundColor DarkGray
    Write-Host "                  " -NoNewline
    Write-Host "• Press Ctrl+C to exit log view" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "  Help            " -NoNewline -ForegroundColor White
    Write-Host "Show this help message" -ForegroundColor Gray
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Cyan
    Write-Host "  .\run-local.ps1              " -NoNewline -ForegroundColor Yellow
    Write-Host "# Start in foreground (default)" -ForegroundColor DarkGray
    Write-Host "  .\run-local.ps1 Start        " -NoNewline -ForegroundColor Yellow
    Write-Host "# Start in foreground (explicit)" -ForegroundColor DarkGray
    Write-Host "  .\run-local.ps1 Background   " -NoNewline -ForegroundColor Yellow
    Write-Host "# Start in background" -ForegroundColor DarkGray
    Write-Host "  .\run-local.ps1 Logs         " -NoNewline -ForegroundColor Yellow
    Write-Host "# View logs" -ForegroundColor DarkGray
    Write-Host "  .\run-local.ps1 Stop         " -NoNewline -ForegroundColor Yellow
    Write-Host "# Stop backend" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "HOT CODE DEPLOYMENT:" -ForegroundColor Cyan
    Write-Host "  • START mode (foreground):" -ForegroundColor White
    Write-Host "    - Spring Boot DevTools auto-detects Java class changes" -ForegroundColor Gray
    Write-Host "    - Application restarts automatically on save" -ForegroundColor Gray
    Write-Host "    - Fast reload (seconds, not full restart)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  • BACKGROUND mode:" -ForegroundColor White
    Write-Host "    - Changes NOT auto-deployed" -ForegroundColor Gray
    Write-Host "    - Must stop and restart manually" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  • VS Code Agent mode:" -ForegroundColor White
    Write-Host "    - Changes are staged but NOT deployed" -ForegroundColor Gray
    Write-Host "    - Only applied when you ACCEPT the changes" -ForegroundColor Gray
    Write-Host "    - Prevents untested code from running" -ForegroundColor Gray
    Write-Host ""
    Write-Host "REQUIREMENTS:" -ForegroundColor Cyan
    Write-Host "  ✓ Java 21 (JDK)" -ForegroundColor White
    Write-Host "  ✓ .env file (run './dev.ps1 Setup' if missing)" -ForegroundColor White
    Write-Host "  ✓ SQL Server on localhost:1433" -ForegroundColor White
    Write-Host "  ✓ Keycloak on localhost:9090" -ForegroundColor White
    Write-Host ""
    Write-Host "NOTES:" -ForegroundColor Cyan
    Write-Host "  • PID saved to:   .backend-local.pid" -ForegroundColor White
    Write-Host "  • Logs saved to:  backend-local.log" -ForegroundColor White
    Write-Host "  • Health check:   http://localhost:8080/actuator/health" -ForegroundColor White
    Write-Host ""
}

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

# Function to check if port is in use
function Test-PortInUse {
    param([int]$Port)
    
    try {
        $connection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        return $connection -ne $null
    } catch {
        return $false
    }
}

# Function to find and stop Java processes on port 8080
function Stop-JavaOnPort {
    param([int]$Port = 8080)
    
    # Get all Java processes
    $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
    
    if (-not $javaProcesses) {
        Write-InfoMsg "No Java processes found"
        return $true
    }
    
    # Try to find processes using the port
    $processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | 
                 Where-Object { $_.State -eq 'Listen' -or $_.State -eq 'Established' } |
                 Select-Object -ExpandProperty OwningProcess -Unique |
                 Where-Object { $_ -gt 0 }  # Exclude PID 0 (System Idle)
    
    $killed = $false
    
    if ($processes) {
        # Stop processes that are actually using the port
        foreach ($procId in $processes) {
            try {
                $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($process -and $process.Name -match 'java') {
                    Write-InfoMsg "Found Java process on port ${Port}: $($process.Name) (PID: $procId)"
                    Stop-Process -Id $procId -Force -ErrorAction Stop
                    Write-SuccessMsg "Stopped process $($process.Name) (PID: $procId)"
                    $killed = $true
                }
            } catch {
                $errorMsg = $_.Exception.Message
                Write-ErrorMsg "Failed to stop process ${procId}: $errorMsg"
            }
        }
    } else {
        # Fallback: Kill all Java processes as they might be holding the port
        Write-InfoMsg "No specific processes found on port $Port (might be in TIME_WAIT state)"
        Write-InfoMsg "Checking all Java processes..."
        
        foreach ($javaProc in $javaProcesses) {
            # Check if this Java process is listening on our port by checking command line
            try {
                $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($javaProc.Id)" -ErrorAction SilentlyContinue).CommandLine
                if ($cmdLine -and ($cmdLine -match "spring-boot" -or $cmdLine -match "backend")) {
                    Write-InfoMsg "Found Spring Boot Java process: $($javaProc.Name) (PID: $($javaProc.Id))"
                    Stop-Process -Id $javaProc.Id -Force -ErrorAction Stop
                    Write-SuccessMsg "Stopped Java process (PID: $($javaProc.Id))"
                    $killed = $true
                }
            } catch {
                Write-ErrorMsg "Failed to stop Java process $($javaProc.Id): $($_.Exception.Message)"
            }
        }
    }
    
    if ($killed) {
        # Wait for port to be released
        Start-Sleep -Seconds 3
        
        # Check if port is still in use (ignoring TIME_WAIT states)
        $stillListening = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | 
                          Where-Object { $_.State -eq 'Listen' }
        
        if ($stillListening) {
            Write-ErrorMsg "Port $Port is still in use after stopping processes"
            return $false
        } else {
            Write-SuccessMsg "Port $Port is now available"
            return $true
        }
    }
    
    return $true
}

# Function to stop running backend
function Stop-Backend {
    $stopped = $false
    $port = Get-AppPort
    
    # Check PID file first (for background processes)
    if (Test-Path $PidFile) {
        $BackendPid = Get-Content $PidFile
        Write-InfoMsg "Stopping backend process from PID file (PID: $BackendPid)..."
        
        try {
            Stop-Process -Id $BackendPid -Force -ErrorAction SilentlyContinue
            Remove-Item $PidFile -ErrorAction SilentlyContinue
            Write-SuccessMsg "Backend stopped (from PID file)"
            $stopped = $true
        } catch {
            $errorMsg = $_.Exception.Message
            Write-ErrorMsg "Failed to stop process from PID file: $errorMsg"
        }
    }
    
    # Check if port is in use (for foreground processes or orphaned processes)
    if (Test-PortInUse -Port $port) {
        Write-InfoMsg "Port $port is in use, checking for Java processes..."
        if (Stop-JavaOnPort -Port $port) {
            $stopped = $true
        }
    }
    
    if (-not $stopped) {
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

# Function to run in background
function Run-Background {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host "RAP Backend - Local Development with Maven" -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-InfoMsg "Run mode: BACKGROUND (manual reload)"
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

    # Get configured port
    $port = if ($env:APP_PORT) { [int]$env:APP_PORT } else { 8080 }

    # Display key configuration
    Write-Host ""
    Write-Host "Configuration Summary:" -ForegroundColor Cyan
    Write-Host "  Database:     localhost:1433 (raptordb)" -ForegroundColor White
    Write-Host "  Keycloak:     localhost:9090 (realm: raptor)" -ForegroundColor White
    Write-Host "  Frontend URL: $env:FRONTEND_URL" -ForegroundColor White
    Write-Host "  OIDC Client:  $env:OIDC_CLIENT_ID" -ForegroundColor White
    Write-Host "  Backend Port: $port" -ForegroundColor White
    Write-Host ""

    # Check if port is already in use
    if (Test-PortInUse -Port $port) {
        Write-ErrorMsg "Port $port is already in use!"
        Write-InfoMsg "Attempting to stop existing process..."
        
        if (-not (Stop-JavaOnPort -Port $port)) {
            Write-ErrorMsg "Failed to free port ${port}. Please manually stop the process using it."
            Write-InfoMsg "You can find the process with: Get-NetTCPConnection -LocalPort $port"
            exit 1
        }
    }

    # Stop any existing backend process
    if (Test-Path $PidFile) {
        Write-InfoMsg "Stopping existing backend process..."
        Stop-Backend
    }

    # Change to backend directory
    Push-Location $BackendDir

    try {
        # Run in background
        Write-InfoMsg "Starting backend in background..."
        Write-Host ""
        Write-Host "  Note: Hot reload NOT available in background mode" -ForegroundColor Yellow
        Write-Host "        Code changes require manual restart" -ForegroundColor Yellow
        Write-Host ""
        
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
        Write-Host "  View logs:  .\run-local.ps1 Logs" -ForegroundColor White
        Write-Host "  Stop:       .\run-local.ps1 Stop" -ForegroundColor White
        Write-Host "  Help:       .\run-local.ps1 Help" -ForegroundColor White
        Write-Host "  Or use:     Get-Content backend-local.log -Wait" -ForegroundColor White
        Write-Host ""
        
        # Wait for backend to start
        Write-InfoMsg "Waiting for backend to start (15 seconds)..."
        Start-Sleep -Seconds 15
        
        # Check if process is still running
        if (Get-Process -Id $process.Id -ErrorAction SilentlyContinue) {
            # Test health endpoint
            try {
                $healthUrl = "http://localhost:${port}/actuator/health"
                $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 5 -ErrorAction Stop
                $health = $response.Content | ConvertFrom-Json
                
                if ($health.status -eq "UP") {
                    Write-SuccessMsg "Backend is healthy and ready!"
                    Write-InfoMsg "Health check: $healthUrl"
                } else {
                    Write-ErrorMsg "Backend is running but not healthy: $($health.status)"
                    Write-InfoMsg "Check logs: .\run-local.ps1 Logs"
                }
            } catch {
                Write-ErrorMsg "Backend is running but health check failed"
                Write-InfoMsg "It may still be starting up. Check logs: .\run-local.ps1 Logs"
            }
        } else {
            Write-ErrorMsg "Backend process stopped unexpectedly"
            Write-InfoMsg "Check logs: Get-Content $LogFile"
            Remove-Item $PidFile -ErrorAction SilentlyContinue
            exit 1
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
    Write-Host "  Health:     http://localhost:${port}/actuator/health" -ForegroundColor White
    Write-Host "  Auth Login: http://localhost:${port}/auth/login" -ForegroundColor White
    Write-Host "  API:        http://localhost:${port}/api/applications" -ForegroundColor White
    Write-Host ""
}

# Function to run in foreground (start)
function Start-Foreground {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host "RAP Backend - Local Development with Maven" -ForegroundColor Yellow
    Write-Host "==================================================" -ForegroundColor Yellow
    Write-Host ""
    Write-InfoMsg "Run mode: FOREGROUND (hot reload enabled)"
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

    # Get configured port
    $port = if ($env:APP_PORT) { [int]$env:APP_PORT } else { 8080 }

    # Display key configuration
    Write-Host ""
    Write-Host "Configuration Summary:" -ForegroundColor Cyan
    Write-Host "  Database:     localhost:1433 (raptordb)" -ForegroundColor White
    Write-Host "  Keycloak:     localhost:9090 (realm: raptor)" -ForegroundColor White
    Write-Host "  Frontend URL: $env:FRONTEND_URL" -ForegroundColor White
    Write-Host "  OIDC Client:  $env:OIDC_CLIENT_ID" -ForegroundColor White
    Write-Host "  Backend Port: $port" -ForegroundColor White
    Write-Host ""

    # Check if port is already in use
    if (Test-PortInUse -Port $port) {
        Write-ErrorMsg "Port $port is already in use!"
        Write-InfoMsg "Attempting to stop existing process..."
        
        if (-not (Stop-JavaOnPort -Port $port)) {
            Write-ErrorMsg "Failed to free port ${port}. Please manually stop the process using it."
            Write-InfoMsg "You can find the process with: Get-NetTCPConnection -LocalPort $port"
            exit 1
        }
    }

    # Stop any existing backend process
    if (Test-Path $PidFile) {
        Write-InfoMsg "Stopping existing backend process..."
        Stop-Backend
    }

    # Change to backend directory
    Push-Location $BackendDir

    try {
        # Run in foreground (default)
        Write-InfoMsg "Starting backend in foreground mode..."
        Write-Host ""
        Write-Host "  ✓ Hot reload ENABLED via Spring Boot DevTools" -ForegroundColor Green
        Write-Host "    - Java class changes auto-detected" -ForegroundColor Gray
        Write-Host "    - Application restarts automatically" -ForegroundColor Gray
        Write-Host "    - Fast reload (3-5 seconds typical)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  Note: In VS Code Agent mode, changes are staged" -ForegroundColor Yellow
        Write-Host "        and NOT deployed until you accept them" -ForegroundColor Yellow
        Write-Host ""
        Write-InfoMsg "Press Ctrl+C to stop"
        Write-Host ""
        
        # Register cleanup handler for Ctrl+C
        $port = Get-AppPort
        $cleanupScript = {
            param($Port)
            Write-Host ""
            Write-Host "[INFO] Cleaning up processes on port $Port..." -ForegroundColor Cyan
            
            # Find and kill Java processes on the port
            $processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | 
                         Select-Object -ExpandProperty OwningProcess -Unique
            
            if ($processes) {
                foreach ($procId in $processes) {
                    try {
                        $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
                        if ($process -and $process.Name -match 'java') {
                            Write-Host "[INFO] Stopping Java process (PID: $procId)..." -ForegroundColor Cyan
                            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                        }
                    } catch {
                        # Ignore errors during cleanup
                    }
                }
            }
        }
        
        try {
            & .\mvnw.cmd spring-boot:run
        } finally {
            # Always cleanup on exit (Ctrl+C or normal exit)
            & $cleanupScript -Port $port
        }
        
        # This line only executes if Maven exits (Ctrl+C or error)
        $exitCode = $LASTEXITCODE
        
        if ($exitCode -eq 0) {
            Write-Host ""
            Write-InfoMsg "Backend stopped gracefully"
        } else {
            Write-Host ""
            Write-ErrorMsg "Backend exited with error code: $exitCode"
        }
    } finally {
        Pop-Location
    }
}

# Main command router
switch ($Command.ToLower()) {
    "help" {
        Show-Help
        exit 0
    }
    "stop" {
        Stop-Backend
        exit 0
    }
    "logs" {
        Show-Logs
        exit 0
    }
    "background" {
        Run-Background
        exit 0
    }
    "start" {
        Start-Foreground
        exit 0
    }
    default {
        # Default to Start (foreground)
        Start-Foreground
        exit 0
    }
}
