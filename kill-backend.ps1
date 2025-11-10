#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Quick helper to kill backend processes on port 8080

.DESCRIPTION
    This script forcefully stops all Java processes using port 8080.
    Use this when Ctrl+C doesn't properly kill the backend.

.EXAMPLE
    .\kill-backend.ps1
    # Kills all processes on port 8080
#>

param(
    [Parameter()]
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

function Write-InfoMsg { param($msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-SuccessMsg { param($msg) Write-Host "[SUCCESS] $msg" -ForegroundColor Green }
function Write-ErrorMsg { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

Write-Host ""
Write-Host "==================================================" -ForegroundColor Yellow
Write-Host "Kill Backend on Port $Port" -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Yellow
Write-Host ""

# Find all Java processes first
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue

if (-not $javaProcesses) {
    Write-InfoMsg "No Java processes found"
    exit 0
}

Write-InfoMsg "Found $($javaProcesses.Count) Java process(es) running"

# Find processes specifically listening on the port (not TIME_WAIT)
$processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | 
             Where-Object { $_.State -eq 'Listen' -or $_.State -eq 'Established' } |
             Select-Object -ExpandProperty OwningProcess -Unique |
             Where-Object { $_ -gt 0 }  # Exclude PID 0

$killed = $false

if ($processes) {
    Write-InfoMsg "Found $($processes.Count) process(es) using port $Port"
    Write-Host ""
    
    foreach ($procId in $processes) {
        try {
            $process = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($process -and $process.Name -match 'java') {
                Write-InfoMsg "Killing Java process: $($process.Name) (PID: $procId)"
                Stop-Process -Id $procId -Force -ErrorAction Stop
                Write-SuccessMsg "✓ Killed $($process.Name) (PID: $procId)"
                $killed = $true
            }
        } catch {
            Write-ErrorMsg "Failed to kill process ${procId}: $($_.Exception.Message)"
        }
    }
} else {
    Write-InfoMsg "No processes actively listening on port $Port"
    Write-InfoMsg "Checking Java processes for Spring Boot application..."
    Write-Host ""
    
    # Fallback: Check all Java processes for Spring Boot
    foreach ($javaProc in $javaProcesses) {
        try {
            $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($javaProc.Id)" -ErrorAction SilentlyContinue).CommandLine
            if ($cmdLine -and ($cmdLine -match "spring-boot" -or $cmdLine -match "backend" -or $cmdLine -match "8080")) {
                Write-InfoMsg "Killing Spring Boot Java process (PID: $($javaProc.Id))"
                Stop-Process -Id $javaProc.Id -Force -ErrorAction Stop
                Write-SuccessMsg "✓ Killed Java process (PID: $($javaProc.Id))"
                $killed = $true
            }
        } catch {
            Write-ErrorMsg "Failed to kill Java process $($javaProc.Id): $($_.Exception.Message)"
        }
    }
}

Write-Host ""

# Wait and verify
Start-Sleep -Seconds 2

$remainingProcesses = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
if ($remainingProcesses) {
    Write-ErrorMsg "Port $Port is still in use!"
    Write-InfoMsg "Run this script again or restart your computer"
} else {
    Write-SuccessMsg "✓ Port $Port is now free"
}

Write-Host ""
