<#
.SYNOPSIS
    Automated setup script to restore OIDC configuration after Docker cleanup.

.DESCRIPTION
    This script restores the complete OAuth2/OIDC setup after you've cleared Docker
    images, containers, or volumes. It will:
    
    1. Start all required services (database, Keycloak, backend)
    2. Wait for Keycloak to be ready
    3. Create Keycloak client with proper configuration
    4. Create test user account
    5. Configure Keycloak frontendUrl
    6. Update .env file with new client secret
    7. Restart backend with updated configuration
    
.PARAMETER SkipServiceStart
    Skip starting Docker services (use if services are already running)

.PARAMETER TestUserPassword
    Password for the test user (default: Arif@123456789012)

.EXAMPLE
    .\setup-after-docker-cleanup.ps1
    # Full setup from scratch

.EXAMPLE
    .\setup-after-docker-cleanup.ps1 -SkipServiceStart
    # Only reconfigure Keycloak (services already running)

.NOTES
    This script should be run from the backend directory.
    Requires: Docker Desktop running, PowerShell 5.1+
#>

param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipServiceStart,
    
    [Parameter(Mandatory=$false)]
    [string]$TestUserPassword = "Arif@123456789012"
)

# Color output functions
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Write-Error-Message {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Section {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Yellow
    Write-Host $Message -ForegroundColor Yellow
    Write-Host "========================================`n" -ForegroundColor Yellow
}

# Configuration
$ScriptDir = $PSScriptRoot
$BackendDir = Split-Path (Split-Path $ScriptDir -Parent) -Parent
$EnvFile = Join-Path $BackendDir ".env"
$DevScriptPath = Join-Path $BackendDir "dev.ps1"
$ScriptsLocalDir = $ScriptDir

Write-Section "RAP Backend - Post Docker Cleanup Setup"

# Step 1: Start services if needed
if (-not $SkipServiceStart) {
    Write-Info "Step 1/7: Starting Docker services..."
    
    Push-Location $BackendDir
    & $DevScriptPath Dev-Start
    $exitCode = $LASTEXITCODE
    Pop-Location
    
    if ($exitCode -ne 0) {
        Write-Error-Message "Failed to start services"
        exit 1
    }
    
    Write-Success "Services started"
    
    Write-Info "Waiting for Keycloak to be ready (30 seconds)..."
    Start-Sleep -Seconds 30
} else {
    Write-Info "Step 1/7: Skipping service start (using existing services)"
}

# Step 2: Verify Keycloak is accessible
Write-Info "Step 2/7: Verifying Keycloak is accessible..."

try {
    $response = Invoke-WebRequest -Uri "http://localhost:9090/health/ready" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Success "Keycloak is ready"
} catch {
    Write-Error-Message "Keycloak is not accessible. Please ensure Keycloak container is running."
    Write-Info "Try running: docker ps | Select-String keycloak"
    exit 1
}

# Step 3: Configure Keycloak client and create user
Write-Info "Step 3/7: Configuring Keycloak client and creating test user..."

$clientScriptPath = Join-Path $ScriptsLocalDir "configure-keycloak-client.ps1"

if (-not (Test-Path $clientScriptPath)) {
    Write-Error-Message "Client configuration script not found: $clientScriptPath"
    exit 1
}

Write-Info "Running: $clientScriptPath"
$output = & $clientScriptPath -UserPassword $TestUserPassword 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Error-Message "Failed to configure Keycloak client"
    Write-Host $output
    exit 1
}

Write-Host $output

# Extract client secret from output
$clientSecret = $null
foreach ($line in $output) {
    if ($line -match "Client Secret:\s*(.+)") {
        $clientSecret = $matches[1].Trim()
        break
    }
}

if (-not $clientSecret) {
    Write-Error-Message "Could not extract client secret from configuration script output"
    exit 1
}

Write-Success "Keycloak client configured"
Write-Info "Client Secret: $clientSecret"

# Step 4: Configure Keycloak frontendUrl
Write-Info "Step 4/7: Configuring Keycloak frontendUrl..."

$frontendUrlScriptPath = Join-Path $ScriptsLocalDir "configure-keycloak-frontend-url.ps1"

if (-not (Test-Path $frontendUrlScriptPath)) {
    Write-Error-Message "FrontendUrl configuration script not found: $frontendUrlScriptPath"
    exit 1
}

$output = & $frontendUrlScriptPath 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Error-Message "Failed to configure Keycloak frontendUrl"
    Write-Host $output
    exit 1
}

Write-Host $output
Write-Success "Keycloak frontendUrl configured"

# Step 5: Update .env file with new client secret
Write-Info "Step 5/7: Updating .env file with new client secret..."

if (-not (Test-Path $EnvFile)) {
    Write-Error-Message ".env file not found: $EnvFile"
    exit 1
}

# Read .env file
$envContent = Get-Content $EnvFile -Raw

# Replace client secret
$envContent = $envContent -replace '(OIDC_CLIENT_SECRET=).*', "`$1$clientSecret"

# Write back to .env
Set-Content -Path $EnvFile -Value $envContent -NoNewline

Write-Success ".env file updated with new client secret"

# Step 6: Verify backend container is running
Write-Info "Step 6/7: Verifying backend container status..."

$backendContainer = docker ps --filter "name=rap-backend" --format "{{.Names}}" 2>$null

if (-not $backendContainer) {
    Write-Error-Message "Backend container is not running"
    Write-Info "Starting backend container..."
    Push-Location $BackendDir
    docker-compose up -d backend
    $exitCode = $LASTEXITCODE
    Pop-Location
    
    if ($exitCode -ne 0) {
        Write-Error-Message "Failed to start backend container"
        exit 1
    }
    
    Write-Success "Backend container started"
} else {
    Write-Success "Backend container is running"
}

# Step 7: Restart backend to pick up new client secret
Write-Info "Step 7/7: Restarting backend with new configuration..."

Push-Location $BackendDir
docker-compose restart backend | Out-Null
$exitCode = $LASTEXITCODE
Pop-Location

if ($exitCode -ne 0) {
    Write-Error-Message "Failed to restart backend"
    exit 1
}

Write-Success "Backend restarted"

Write-Info "Waiting for backend to be ready (20 seconds)..."
Start-Sleep -Seconds 20

# Verify backend is healthy
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method GET -TimeoutSec 5 -ErrorAction Stop
    $health = $response.Content | ConvertFrom-Json
    
    if ($health.status -eq "UP") {
        Write-Success "Backend is healthy"
    } else {
        Write-Error-Message "Backend health check failed: $($health.status)"
    }
} catch {
    Write-Error-Message "Backend is not accessible"
    Write-Info "Check logs with: docker logs rap-backend --tail 50"
}

# Summary
Write-Section "Setup Complete!"

Write-Host "Configuration Summary:" -ForegroundColor Cyan
Write-Host "  Keycloak URL:    http://localhost:9090" -ForegroundColor White
Write-Host "  Backend URL:     http://localhost:8080" -ForegroundColor White
Write-Host "  Frontend URL:    http://localhost:4200" -ForegroundColor White
Write-Host ""
Write-Host "Test User Credentials:" -ForegroundColor Cyan
Write-Host "  Username:        user@raptor.local" -ForegroundColor White
Write-Host "  Password:        $TestUserPassword" -ForegroundColor White
Write-Host ""
Write-Host "OIDC Configuration:" -ForegroundColor Cyan
Write-Host "  Client ID:       raptor-client" -ForegroundColor White
Write-Host "  Client Secret:   $clientSecret" -ForegroundColor White
Write-Host "  Realm:           raptor" -ForegroundColor White
Write-Host ""

Write-Success "You can now test the OAuth2 flow:"
Write-Host "  1. Open browser to http://localhost:4200"
Write-Host "  2. Click Login button"
Write-Host "  3. Enter credentials: user@raptor.local / $TestUserPassword"
Write-Host ""

Write-Info "To run advanced tests:"
Write-Host "  .\scripts\local\test-oidc-flow-advanced.ps1"
Write-Host ""
