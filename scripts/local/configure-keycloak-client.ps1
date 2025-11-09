#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Configure Keycloak client for OAuth2 and Direct Grant testing

.DESCRIPTION
    This script:
    1. Authenticates to Keycloak admin API
    2. Finds the raptor-client by clientId
    3. Updates configuration to enable:
       - Direct Access Grants (password flow for testing)
       - Client authentication (confidential client)
       - Standard OAuth2 flow
    4. Regenerates client secret if needed
    5. Displays the client secret

.PARAMETER KeycloakUrl
    Keycloak server URL (default: http://localhost:9090)

.PARAMETER Realm
    Keycloak realm name (default: raptor)

.PARAMETER ClientId
    Client ID to configure (default: raptor-client)

.PARAMETER AdminUsername
    Keycloak admin username (default: admin)

.PARAMETER AdminPassword
    Keycloak admin password (default: admin)

.EXAMPLE
    .\configure-keycloak-client.ps1

.EXAMPLE
    .\configure-keycloak-client.ps1 -ClientId my-client
#>

param(
    [string]$KeycloakUrl = "http://localhost:9090",
    [string]$Realm = "raptor",
    [string]$ClientId = "raptor-client",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "=== Configuring Keycloak Client ===" -ForegroundColor Cyan
Write-Host ""

# Step 1: Get admin access token
Write-Host "Step 1: Authenticating as admin..." -ForegroundColor Yellow
try {
    $tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
    $tokenBody = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $AdminUsername
        password = $AdminPassword
    }
    
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded"
    $adminToken = $tokenResponse.access_token
    
    Write-Host "✓ Admin token obtained" -ForegroundColor Green
}
catch {
    Write-Host "✗ Failed to obtain admin token: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Find client by clientId to get internal UUID
Write-Host ""
Write-Host "Step 2: Finding client internal ID..." -ForegroundColor Yellow
try {
    $clientsUrl = "$KeycloakUrl/admin/realms/$Realm/clients"
    $headers = @{
        Authorization = "Bearer $adminToken"
        Accept = "application/json"
    }
    
    $clients = Invoke-RestMethod -Uri $clientsUrl -Method Get -Headers $headers
    $client = $clients | Where-Object { $_.clientId -eq $ClientId }
    
    if (-not $client) {
        Write-Host "✗ Client '$ClientId' not found in realm '$Realm'" -ForegroundColor Red
        exit 1
    }
    
    $clientUuid = $client.id
    Write-Host "✓ Client found: $ClientId (ID: $clientUuid)" -ForegroundColor Green
}
catch {
    Write-Host "✗ Failed to find client: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Update client configuration
Write-Host ""
Write-Host "Step 3: Updating client configuration..." -ForegroundColor Yellow
try {
    $clientUpdateUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$clientUuid"
    
    # Get current configuration first
    $currentConfig = Invoke-RestMethod -Uri $clientUpdateUrl -Method Get -Headers $headers
    
    # Update the configuration
    $currentConfig.directAccessGrantsEnabled = $true
    $currentConfig.publicClient = $false  # Make it confidential (not public)
    $currentConfig.standardFlowEnabled = $true
    $currentConfig.implicitFlowEnabled = $false
    $currentConfig.serviceAccountsEnabled = $false
    
    # Ensure redirect URIs are set
    if (-not $currentConfig.redirectUris) {
        $currentConfig.redirectUris = @()
    }
    
    $requiredRedirects = @(
        "http://localhost:8080/*",
        "http://localhost:4200/*"
    )
    
    foreach ($redirect in $requiredRedirects) {
        if ($currentConfig.redirectUris -notcontains $redirect) {
            $currentConfig.redirectUris += $redirect
        }
    }
    
    # Ensure web origins are set
    if (-not $currentConfig.webOrigins) {
        $currentConfig.webOrigins = @()
    }
    
    if ($currentConfig.webOrigins -notcontains "http://localhost:4200") {
        $currentConfig.webOrigins += "http://localhost:4200"
    }
    
    $updateHeaders = @{
        Authorization = "Bearer $adminToken"
        "Content-Type" = "application/json"
    }
    
    $configJson = $currentConfig | ConvertTo-Json -Depth 10
    Invoke-RestMethod -Uri $clientUpdateUrl -Method Put -Headers $updateHeaders -Body $configJson | Out-Null
    
    Write-Host "✓ Client configuration updated" -ForegroundColor Green
}
catch {
    Write-Host "✗ Failed to update client configuration: $_" -ForegroundColor Red
    Write-Host "Error details: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 4: Get client secret
Write-Host ""
Write-Host "Step 4: Retrieving client secret..." -ForegroundColor Yellow
try {
    $secretUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$clientUuid/client-secret"
    
    $secretResponse = Invoke-RestMethod -Uri $secretUrl -Method Get -Headers $headers
    $clientSecret = $secretResponse.value
    
    if (-not $clientSecret) {
        Write-Host "No secret found. Generating new secret..." -ForegroundColor Yellow
        $secretResponse = Invoke-RestMethod -Uri $secretUrl -Method Post -Headers $headers
        $clientSecret = $secretResponse.value
    }
    
    Write-Host "✓ Client secret retrieved" -ForegroundColor Green
}
catch {
    Write-Host "✗ Failed to retrieve client secret: $_" -ForegroundColor Red
    exit 1
}

# Step 5: Verify configuration
Write-Host ""
Write-Host "Step 5: Verifying configuration..." -ForegroundColor Yellow
try {
    $verifyUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$clientUuid"
    $verifiedConfig = Invoke-RestMethod -Uri $verifyUrl -Method Get -Headers $headers
    
    Write-Host ""
    Write-Host "Client Configuration:" -ForegroundColor Cyan
    Write-Host "  Client ID: $($verifiedConfig.clientId)" -ForegroundColor White
    Write-Host "  Public Client: $($verifiedConfig.publicClient)" -ForegroundColor $(if (-not $verifiedConfig.publicClient) { "Green" } else { "Yellow" })
    Write-Host "  Direct Access Grants: $($verifiedConfig.directAccessGrantsEnabled)" -ForegroundColor $(if ($verifiedConfig.directAccessGrantsEnabled) { "Green" } else { "Red" })
    Write-Host "  Standard Flow: $($verifiedConfig.standardFlowEnabled)" -ForegroundColor $(if ($verifiedConfig.standardFlowEnabled) { "Green" } else { "Red" })
    Write-Host "  Implicit Flow: $($verifiedConfig.implicitFlowEnabled)" -ForegroundColor White
    Write-Host "  Service Accounts: $($verifiedConfig.serviceAccountsEnabled)" -ForegroundColor White
    Write-Host ""
    Write-Host "  Redirect URIs:" -ForegroundColor Cyan
    foreach ($uri in $verifiedConfig.redirectUris) {
        Write-Host "    - $uri" -ForegroundColor White
    }
    Write-Host ""
    Write-Host "  Web Origins:" -ForegroundColor Cyan
    foreach ($origin in $verifiedConfig.webOrigins) {
        Write-Host "    - $origin" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "✓ Configuration verified successfully!" -ForegroundColor Green
    
    # Display the secret prominently
    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Cyan
    Write-Host "CLIENT SECRET" -ForegroundColor Yellow
    Write-Host "==============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  $clientSecret" -ForegroundColor Green
    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Update your .env file with:" -ForegroundColor Yellow
    Write-Host "  OIDC_CLIENT_SECRET=$clientSecret" -ForegroundColor White
    Write-Host ""
}
catch {
    Write-Host "✗ Failed to verify configuration: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Configuration Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "You can now run: .\test-oidc-flow-advanced.ps1" -ForegroundColor Cyan
Write-Host ""
