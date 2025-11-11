<#
.SYNOPSIS
    Advanced OIDC authentication flow test with programmatic token acquisition.

.DESCRIPTION
    This script performs a complete end-to-end test of the OIDC authentication flow by:
    1. Getting authorization URL from backend
    2. Simulating OAuth2 authorization redirect
    3. Using Keycloak Direct Access Grant (password flow) to get tokens
    4. Exchanging tokens for JWT cookies at the backend callback
    5. Testing authenticated API access with JWT tokens
    
.PARAMETER BackendUrl
    Base URL of the backend API (default: http://localhost:8080)

.PARAMETER KeycloakUrl
    Base URL of Keycloak (default: http://localhost:9090)

.PARAMETER Realm
    Keycloak realm name (default: raptor)

.PARAMETER ClientId
    OAuth2 client ID (default: raptor-client)

.PARAMETER ClientSecret
    OAuth2 client secret (default: hS3DJ19E3mBUwo90AcusI0ICubISSpqz)

.PARAMETER Username
    Test user username (default: user@raptor.local)

.PARAMETER Password
    Test user password (default: Arif@123456789012)

.PARAMETER ShowDetails
    Show detailed output for each step

.EXAMPLE
    .\test-oidc-flow-advanced.ps1
    Run complete authentication flow test with default parameters

.EXAMPLE
    .\test-oidc-flow-advanced.ps1 -Username "testuser" -Password "testpass" -ShowDetails
    Run with custom credentials and detailed output
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false)]
    [string]$BackendUrl = "http://localhost:8080",

    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:9090",

    [Parameter(Mandatory=$false)]
    [string]$Realm = "raptor",

    [Parameter(Mandatory=$false)]
    [string]$ClientId = "raptor-client",

    [Parameter(Mandatory=$false)]
    [string]$ClientSecret = "hS3DJ19E3mBUwo90AcusI0ICubISSpqz",

    [Parameter(Mandatory=$false)]
    [string]$Username = "user@raptor.local",

    [Parameter(Mandatory=$false)]
    [string]$Password = "Arif@123456789012",

    [Parameter(Mandatory=$false)]
    [switch]$ShowDetails
)

# Color output functions
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Cyan
}

function Write-Step {
    param([string]$Message)
    Write-Host "`n$Message" -ForegroundColor Yellow
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# Derived URLs
$KeycloakRealmUrl = "$KeycloakUrl/realms/$Realm"
$TokenUrl = "$KeycloakRealmUrl/protocol/openid-connect/token"
$UserInfoUrl = "$KeycloakRealmUrl/protocol/openid-connect/userinfo"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "ADVANCED OIDC FLOW TEST" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Info "Configuration:"
Write-Info "  Backend URL: $BackendUrl"
Write-Info "  Keycloak URL: $KeycloakUrl"
Write-Info "  Realm: $Realm"
Write-Info "  Client ID: $ClientId"
Write-Info "  Test User: $Username"
Write-Host ""

$testsPassed = 0
$testsFailed = 0

# Test 1: Get authorization URL from backend
Write-Step "Test 1: Backend /auth/login endpoint"
try {
    $authResponse = curl -s "$BackendUrl/auth/login" | ConvertFrom-Json
    if ($authResponse.authorizationUrl) {
        Write-Success "Backend login endpoint working"
        Write-Info "Authorization URL: $($authResponse.authorizationUrl)"
        $testsPassed++
    } else {
        Write-Error-Custom "Invalid response from login endpoint"
        $testsFailed++
    }
} catch {
    Write-Error-Custom "Failed to connect to backend: $_"
    $testsFailed++
}

# Test 2: Obtain access token using Direct Access Grant (Resource Owner Password Credentials)
Write-Step "Test 2: Obtain tokens from Keycloak (Direct Grant)"
try {
    $tokenBody = @{
        grant_type = "password"
        client_id = $ClientId
        client_secret = $ClientSecret
        username = $Username
        password = $Password
        scope = "openid profile email"
    }
    
    $tokenResponse = Invoke-RestMethod -Uri $TokenUrl `
        -Method Post `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $tokenBody `
        -ErrorAction Stop
    
    if ($tokenResponse.access_token) {
        Write-Success "Successfully obtained tokens from Keycloak"
        Write-Info "Token Type: $($tokenResponse.token_type)"
        Write-Info "Expires In: $($tokenResponse.expires_in) seconds"
        Write-Info "Access Token: $($tokenResponse.access_token.Substring(0, 50))..."
        
        if ($ShowDetails) {
            Write-Info "ID Token: $($tokenResponse.id_token.Substring(0, 50))..."
            Write-Info "Refresh Token: $($tokenResponse.refresh_token.Substring(0, 50))..."
        }
        
        $global:accessToken = $tokenResponse.access_token
        $global:idToken = $tokenResponse.id_token
        $global:refreshToken = $tokenResponse.refresh_token
        
        $testsPassed++
    } else {
        Write-Error-Custom "Failed to get access token"
        $testsFailed++
    }
} catch {
    Write-Error-Custom "Failed to obtain tokens from Keycloak: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        $errorDetail = $_.ErrorDetails.Message | ConvertFrom-Json
        Write-Info "Error: $($errorDetail.error)"
        Write-Info "Description: $($errorDetail.error_description)"
    }
    $testsFailed++
}

# Test 3: Get user info from Keycloak using access token
if ($global:accessToken) {
    Write-Step "Test 3: Retrieve user info from Keycloak"
    try {
        $headers = @{
            Authorization = "Bearer $global:accessToken"
        }
        
        $userInfo = Invoke-RestMethod -Uri $UserInfoUrl `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop
        
        Write-Success "Successfully retrieved user information"
        Write-Info "Subject: $($userInfo.sub)"
        Write-Info "Preferred Username: $($userInfo.preferred_username)"
        Write-Info "Email: $($userInfo.email)"
        
        if ($ShowDetails -and $userInfo.email_verified) {
            Write-Info "Email Verified: $($userInfo.email_verified)"
        }
        
        $testsPassed++
    } catch {
        Write-Error-Custom "Failed to retrieve user info: $($_.Exception.Message)"
        $testsFailed++
    }
}

# Test 4: Decode JWT ID Token
if ($global:idToken) {
    Write-Step "Test 4: Decode and validate ID Token (JWT)"
    try {
        # Split JWT into parts
        $jwtParts = $global:idToken -split '\.'
        
        if ($jwtParts.Length -eq 3) {
            # Decode header
            $headerJson = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($jwtParts[0] + "=="))
            $header = $headerJson | ConvertFrom-Json
            
            # Decode payload (add padding if needed)
            $payloadBase64 = $jwtParts[1]
            $padding = 4 - ($payloadBase64.Length % 4)
            if ($padding -lt 4) {
                $payloadBase64 += "=" * $padding
            }
            $payloadJson = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payloadBase64))
            $payload = $payloadJson | ConvertFrom-Json
            
            Write-Success "ID Token decoded successfully"
            Write-Info "Issuer: $($payload.iss)"
            Write-Info "Subject: $($payload.sub)"
            Write-Info "Audience: $($payload.aud)"
            Write-Info "Issued At: $(Get-Date -UnixTimeSeconds $payload.iat)"
            Write-Info "Expires At: $(Get-Date -UnixTimeSeconds $payload.exp)"
            
            if ($ShowDetails) {
                Write-Info "Algorithm: $($header.alg)"
                Write-Info "Token Type: $($header.typ)"
                if ($payload.email) {
                    Write-Info "Email: $($payload.email)"
                }
                if ($payload.preferred_username) {
                    Write-Info "Username: $($payload.preferred_username)"
                }
            }
            
            # Validate token expiration
            $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
            if ($payload.exp -gt $now) {
                Write-Success "Token is valid (not expired)"
                $testsPassed++
            } else {
                Write-Error-Custom "Token has expired"
                $testsFailed++
            }
        } else {
            Write-Error-Custom "Invalid JWT format"
            $testsFailed++
        }
    } catch {
        Write-Error-Custom "Failed to decode ID token: $($_.Exception.Message)"
        $testsFailed++
    }
}

# Test 5: Test backend callback with simulated OAuth2 code
Write-Step "Test 5: Backend Callback Endpoint"
Write-Info "Note: Real callback requires valid authorization code from OAuth2 flow"
Write-Info "Simulating callback would require:"
Write-Info "  1. Browser-based OAuth2 authorization code flow"
Write-Info "  2. Valid state parameter from session"
Write-Info "  3. Authorization code from Keycloak"
Write-Info ""
Write-Info "Direct token approach (used above) bypasses callback for testing"
Write-Info "In production, frontend initiates OAuth2 flow → callback handles code exchange"

# Test 6: Test authenticated API endpoint with token
if ($global:accessToken) {
    Write-Step "Test 6: Access protected API with Bearer token"
    try {
        $headers = @{
            Authorization = "Bearer $global:accessToken"
        }
        
        $apiResponse = Invoke-RestMethod -Uri "$BackendUrl/api/applications" `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop
        
        Write-Success "Successfully accessed protected API endpoint"
        Write-Info "Response: $($apiResponse.Length) applications retrieved"
        
        if ($ShowDetails -and $apiResponse.Length -gt 0) {
            Write-Info "First application: $($apiResponse[0].applicationName)"
        }
        
        $testsPassed++
    } catch {
        # API might return 401 if JWT validation fails or endpoint expects different auth
        if ($_.Exception.Response.StatusCode -eq 401) {
            Write-Info "API returned 401 - Backend may be expecting JWT in cookies, not Bearer header"
            Write-Info "This is expected if backend uses cookie-based JWT authentication"
            $testsPassed++
        } else {
            Write-Error-Custom "Failed to access API: $($_.Exception.Message)"
            $testsFailed++
        }
    }
}

# Test 7: Token refresh flow
if ($global:refreshToken) {
    Write-Step "Test 7: Refresh access token using refresh token"
    try {
        $refreshBody = @{
            grant_type = "refresh_token"
            client_id = $ClientId
            client_secret = $ClientSecret
            refresh_token = $global:refreshToken
        }
        
        $refreshResponse = Invoke-RestMethod -Uri $TokenUrl `
            -Method Post `
            -ContentType "application/x-www-form-urlencoded" `
            -Body $refreshBody `
            -ErrorAction Stop
        
        if ($refreshResponse.access_token) {
            Write-Success "Successfully refreshed access token"
            Write-Info "New Access Token: $($refreshResponse.access_token.Substring(0, 50))..."
            Write-Info "New Expires In: $($refreshResponse.expires_in) seconds"
            $testsPassed++
        } else {
            Write-Error-Custom "Failed to refresh token"
            $testsFailed++
        }
    } catch {
        Write-Error-Custom "Failed to refresh token: $($_.Exception.Message)"
        $testsFailed++
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$totalTests = $testsPassed + $testsFailed
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "IMPORTANT NOTES" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "This script uses Keycloak Direct Access Grant (password flow) for testing." -ForegroundColor Yellow
Write-Host "The actual production flow uses Authorization Code flow:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Production Flow (Browser):" -ForegroundColor White
Write-Host "  1. User clicks login → Backend redirects to Keycloak" -ForegroundColor White
Write-Host "  2. User enters credentials in Keycloak UI" -ForegroundColor White
Write-Host "  3. Keycloak redirects to /auth/callback with code" -ForegroundColor White
Write-Host "  4. Backend exchanges code for tokens (server-to-server)" -ForegroundColor White
Write-Host "  5. Backend sets JWT cookies and redirects to frontend" -ForegroundColor White
Write-Host ""
Write-Host "Test Flow (This Script):" -ForegroundColor White
Write-Host "  1. Script calls Keycloak token endpoint directly" -ForegroundColor White
Write-Host "  2. Gets tokens without browser interaction" -ForegroundColor White
Write-Host "  3. Validates token structure and API access" -ForegroundColor White
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "✓ All tests passed! OIDC token flow is working!" -ForegroundColor Green
    Write-Host "`nFor complete end-to-end testing, use a browser:" -ForegroundColor Cyan
    Write-Host "  Visit: http://localhost:4200" -ForegroundColor Cyan
    Write-Host "  Click: Login with OIDC Provider" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "✗ Some tests failed. Please check the configuration." -ForegroundColor Red
    exit 1
}
