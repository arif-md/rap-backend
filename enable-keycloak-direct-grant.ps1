<#
.SYNOPSIS
    Enable Direct Grant (Resource Owner Password Credentials) in Keycloak for testing

.DESCRIPTION
    This script enables the Direct Grant flow in Keycloak's raptor-client configuration.
    This allows programmatic authentication for testing purposes.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:9090",
    
    [Parameter(Mandatory=$false)]
    [string]$Realm = "raptor",
    
    [Parameter(Mandatory=$false)]
    [string]$ClientId = "raptor-client",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminUsername = "admin",
    
    [Parameter(Mandatory=$false)]
    [string]$AdminPassword = "admin"
)

Write-Host "`n=== Enabling Direct Grant in Keycloak ===`n" -ForegroundColor Cyan

# Step 1: Get admin token
Write-Host "Step 1: Authenticating as admin..." -ForegroundColor Yellow
try {
    $tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
    $tokenBody = @{
        grant_type = "password"
        client_id = "admin-cli"
        username = $AdminUsername
        password = $AdminPassword
    }
    
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody
    
    $adminToken = $tokenResponse.access_token
    Write-Host "✓ Admin token obtained" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to get admin token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Get client internal ID
Write-Host "`nStep 2: Finding client internal ID..." -ForegroundColor Yellow
try {
    $headers = @{
        Authorization = "Bearer $adminToken"
        Accept = "application/json"
    }
    
    $clientsUrl = "$KeycloakUrl/admin/realms/$Realm/clients"
    $clients = Invoke-RestMethod -Uri $clientsUrl -Headers $headers -Method Get
    
    $client = $clients | Where-Object { $_.clientId -eq $ClientId }
    
    if ($client) {
        $clientUuid = $client.id
        Write-Host "✓ Client found: $ClientId (ID: $clientUuid)" -ForegroundColor Green
    } else {
        Write-Host "✗ Client not found: $ClientId" -ForegroundColor Red
        Write-Host "Available clients:" -ForegroundColor Yellow
        $clients | ForEach-Object { Write-Host "  - $($_.clientId)" -ForegroundColor Gray }
        exit 1
    }
} catch {
    Write-Host "✗ Failed to find client: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Update client to enable Direct Grant
Write-Host "`nStep 3: Enabling Direct Grant flow..." -ForegroundColor Yellow
try {
    $clientUrl = "$KeycloakUrl/admin/realms/$Realm/clients/$clientUuid"
    
    # Get current client configuration
    $currentConfig = Invoke-RestMethod -Uri $clientUrl -Headers $headers -Method Get
    
    # Update configuration
    $currentConfig.directAccessGrantsEnabled = $true
    $currentConfig.standardFlowEnabled = $true  # Keep authorization code flow
    
    # Convert to JSON and update
    $configJson = $currentConfig | ConvertTo-Json -Depth 10
    
    Invoke-RestMethod -Uri $clientUrl `
        -Headers $headers `
        -Method Put `
        -ContentType "application/json" `
        -Body $configJson | Out-Null
    
    Write-Host "✓ Direct Grant enabled for client: $ClientId" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to update client: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

# Step 4: Verify configuration
Write-Host "`nStep 4: Verifying configuration..." -ForegroundColor Yellow
try {
    $verifyConfig = Invoke-RestMethod -Uri $clientUrl -Headers $headers -Method Get
    
    Write-Host "`nClient Configuration:" -ForegroundColor Cyan
    Write-Host "  Client ID: $($verifyConfig.clientId)" -ForegroundColor White
    Write-Host "  Direct Access Grants: $($verifyConfig.directAccessGrantsEnabled)" -ForegroundColor $(if ($verifyConfig.directAccessGrantsEnabled) { "Green" } else { "Red" })
    Write-Host "  Standard Flow: $($verifyConfig.standardFlowEnabled)" -ForegroundColor $(if ($verifyConfig.standardFlowEnabled) { "Green" } else { "Red" })
    Write-Host "  Implicit Flow: $($verifyConfig.implicitFlowEnabled)" -ForegroundColor White
    Write-Host "  Service Accounts: $($verifyConfig.serviceAccountsEnabled)" -ForegroundColor White
    
    if ($verifyConfig.directAccessGrantsEnabled) {
        Write-Host "`n✓ Configuration verified successfully!" -ForegroundColor Green
    } else {
        Write-Host "`n✗ Direct Grant is still disabled" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Failed to verify configuration: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Direct Grant Enabled Successfully ===" -ForegroundColor Green
Write-Host "`nYou can now run: .\test-oidc-flow-advanced.ps1" -ForegroundColor Cyan
Write-Host ""
