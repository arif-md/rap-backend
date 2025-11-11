<#
.SYNOPSIS
    Test authenticated endpoints by simulating browser-based OIDC authentication flow.

.DESCRIPTION
    This script performs a complete browser-like authentication flow:
    1. Initiates OAuth2 flow with backend
    2. Follows redirect to Keycloak
    3. Submits login form with credentials
    4. Follows callback redirect back to backend
    5. Uses established session to call protected API endpoints
    
.PARAMETER Endpoint
    The API endpoint to test (e.g., /api/workflow/tasks, /api/permits/my)

.PARAMETER Username
    Keycloak username (default: user@raptor.local)

.PARAMETER Password
    Keycloak password (default: Arif@123456789012)

.PARAMETER BackendUrl
    Base URL of the backend API (default: http://localhost:8080)

.PARAMETER KeycloakUrl
    Base URL of Keycloak (default: http://localhost:9090)

.PARAMETER QueryParams
    Additional query parameters as a string (e.g., "page=0&size=10")

.PARAMETER ShowDetails
    Show detailed output including response headers and body

.EXAMPLE
    .\test-authenticated-endpoint.ps1 -Endpoint "/api/workflow/tasks" -QueryParams "page=0&size=10"
    Test workflow tasks endpoint with pagination

.EXAMPLE
    .\test-authenticated-endpoint.ps1 -Endpoint "/api/permits/my" -QueryParams "page=0&size=10" -ShowDetails
    Test permits endpoint with detailed output

.EXAMPLE
    .\test-authenticated-endpoint.ps1 -Endpoint "/api/applications/my" -Username "admin@raptor.local" -Password "AdminPass123"
    Test with custom credentials
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [string]$Endpoint,

    [Parameter(Mandatory=$false)]
    [string]$Username = "user@raptor.local",

    [Parameter(Mandatory=$false)]
    [string]$Password = "Arif@123456789012",

    [Parameter(Mandatory=$false)]
    [string]$BackendUrl = "http://localhost:8080",

    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "http://localhost:9090",

    [Parameter(Mandatory=$false)]
    [string]$QueryParams = "",

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

function Write-Detail {
    param([string]$Message)
    if ($ShowDetails) {
        Write-Host "  $Message" -ForegroundColor Gray
    }
}

# Build query string from parameter
function Build-QueryString {
    param([string]$Params)
    
    if ([string]::IsNullOrWhiteSpace($Params)) {
        return ""
    }
    
    if ($Params.StartsWith("?")) {
        return $Params
    }
    
    return "?$Params"
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "AUTHENTICATED ENDPOINT TEST" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Info "Configuration:"
Write-Info "  Backend URL: $BackendUrl"
Write-Info "  Keycloak URL: $KeycloakUrl"
Write-Info "  Username: $Username"
Write-Info "  Endpoint: $Endpoint"
if (-not [string]::IsNullOrWhiteSpace($QueryParams)) {
    Write-Info "  Query Parameters: $(Build-QueryString $QueryParams)"
}
Write-Host ""

# Create a session to maintain cookies
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

try {
    # Step 1: Initiate OAuth2 flow
    Write-Step "Step 1: Initiating OAuth2 authentication flow"
    
    $oauth2Url = "$BackendUrl/oauth2/authorization/oidc-provider"
    Write-Detail "Requesting: $oauth2Url"
    
    $response = Invoke-WebRequest -Uri $oauth2Url `
        -Method Get `
        -WebSession $session `
        -MaximumRedirection 0 `
        -ErrorAction Stop
    
    Write-Error-Custom "Expected redirect to Keycloak, but got: $($response.StatusCode)"
    exit 1
    
} catch {
    if ($_.Exception.Response.StatusCode -eq 302 -or $_.Exception.Response.StatusCode -eq 'Found') {
        $keycloakAuthUrl = $_.Exception.Response.Headers.Location.AbsoluteUri
        Write-Success "Redirected to Keycloak authentication"
        Write-Detail "Keycloak URL: $keycloakAuthUrl"
        
        # Step 2: Load Keycloak login page
        Write-Step "Step 2: Loading Keycloak login page"
        
        try {
            $loginPageResponse = Invoke-WebRequest -Uri $keycloakAuthUrl `
                -Method Get `
                -WebSession $session `
                -ErrorAction Stop
            
            Write-Success "Login page loaded successfully"
            
            # Extract form action URL
            if ($loginPageResponse.Content -match 'action="([^"]+)"') {
                $formActionUrl = $matches[1]
                # Decode HTML entities
                $formActionUrl = [System.Web.HttpUtility]::HtmlDecode($formActionUrl)
                Write-Detail "Form action URL: $formActionUrl"
            } else {
                Write-Error-Custom "Could not find login form action URL"
                exit 1
            }
            
            # Step 3: Submit login credentials
            Write-Step "Step 3: Submitting login credentials"
            
            $loginBody = @{
                username = $Username
                password = $Password
            }
            
            $loginResponse = Invoke-WebRequest -Uri $formActionUrl `
                -Method Post `
                -Body $loginBody `
                -WebSession $session `
                -MaximumRedirection 0 `
                -ErrorAction Stop
            
            Write-Error-Custom "Expected redirect after login, but got: $($loginResponse.StatusCode)"
            exit 1
            
        } catch {
            if ($_.Exception.Response.StatusCode -eq 302 -or $_.Exception.Response.StatusCode -eq 'Found') {
                $callbackUrl = $_.Exception.Response.Headers.Location.AbsoluteUri
                Write-Success "Login successful, redirecting to callback"
                Write-Detail "Callback URL: $callbackUrl"
                
                # Step 4: Follow callback to backend
                Write-Step "Step 4: Processing OAuth2 callback"
                
                try {
                    $callbackResponse = Invoke-WebRequest -Uri $callbackUrl `
                        -Method Get `
                        -WebSession $session `
                        -MaximumRedirection 5 `
                        -ErrorAction Stop
                    
                    Write-Success "Authentication completed successfully"
                    Write-Detail "Session established with backend"
                    
                    # Step 5: Call the protected endpoint
                    Write-Step "Step 5: Calling protected endpoint"
                    
                    $queryString = Build-QueryString $QueryParams
                    $fullEndpointUrl = "$BackendUrl$Endpoint$queryString"
                    
                    Write-Detail "Requesting: $fullEndpointUrl"
                    
                    $apiResponse = Invoke-WebRequest -Uri $fullEndpointUrl `
                        -Method Get `
                        -WebSession $session `
                        -ErrorAction Stop
                    
                    Write-Success "API call successful!"
                    Write-Host "`n========================================" -ForegroundColor Green
                    Write-Host "RESPONSE" -ForegroundColor Green
                    Write-Host "========================================" -ForegroundColor Green
                    Write-Info "Status Code: $($apiResponse.StatusCode) $($apiResponse.StatusDescription)"
                    Write-Info "Content Type: $($apiResponse.Headers['Content-Type'])"
                    
                    if ($ShowDetails) {
                        Write-Host "`nResponse Headers:" -ForegroundColor Yellow
                        foreach ($header in $apiResponse.Headers.Keys) {
                            $headerValue = $apiResponse.Headers[$header]
                            Write-Detail "$header`: $headerValue"
                        }
                    }
                    
                    Write-Host "`nResponse Body:" -ForegroundColor Yellow
                    try {
                        $jsonResponse = $apiResponse.Content | ConvertFrom-Json
                        $jsonResponse | ConvertTo-Json -Depth 10 | Write-Host
                    } catch {
                        Write-Host $apiResponse.Content
                    }
                    
                    Write-Host "`n========================================" -ForegroundColor Green
                    Write-Success "Test completed successfully!"
                    Write-Host "========================================`n" -ForegroundColor Green
                    
                    exit 0
                    
                } catch {
                    Write-Error-Custom "Failed to complete callback: $($_.Exception.Message)"
                    if ($ShowDetails) {
                        Write-Detail "Error details: $_"
                    }
                    exit 1
                }
                
            } else {
                Write-Error-Custom "Login failed: $($_.Exception.Message)"
                if ($ShowDetails) {
                    Write-Detail "Error details: $_"
                }
                exit 1
            }
        }
        
    } else {
        Write-Error-Custom "Unexpected response during OAuth2 initiation: $($_.Exception.Message)"
        if ($ShowDetails) {
            Write-Detail "Error details: $_"
        }
        exit 1
    }
}
