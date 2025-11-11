# Testing Authenticated Endpoints

## Quick Start

The `test-authenticated-endpoint.ps1` script simulates a complete browser-based OIDC authentication flow and calls protected API endpoints.

## Basic Usage

```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "<endpoint-path>" -QueryParams "<query-string>"
```

## Examples

### Test Workflow Tasks
```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "/api/workflow/tasks" -QueryParams "page=0&size=10"
```

### Test Permits
```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "/api/permits/my" -QueryParams "page=0&size=10"
```

### Test Applications
```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "/api/applications/my" -QueryParams "page=0&size=10"
```

### With Detailed Output
```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "/api/applications/my" -QueryParams "page=0&size=10" -ShowDetails
```

### With Custom Credentials
```powershell
pwsh -File scripts\local\test-authenticated-endpoint.ps1 `
    -Endpoint "/api/workflow/tasks" `
    -Username "admin@raptor.local" `
    -Password "AdminPassword123" `
    -QueryParams "page=0&size=10"
```

## Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `-Endpoint` | Yes | - | The API endpoint to test (e.g., `/api/workflow/tasks`) |
| `-QueryParams` | No | `""` | Query string parameters (e.g., `"page=0&size=10"`) |
| `-Username` | No | `user@raptor.local` | Keycloak username |
| `-Password` | No | `Arif@123456789012` | Keycloak password |
| `-BackendUrl` | No | `http://localhost:8080` | Backend base URL |
| `-KeycloakUrl` | No | `http://localhost:9090` | Keycloak base URL |
| `-ShowDetails` | No | `false` | Show detailed output including headers |

## How It Works

The script performs a complete browser-like authentication flow:

1. **Initiate OAuth2 Flow**: Requests `/oauth2/authorization/oidc-provider` from backend
2. **Redirect to Keycloak**: Follows redirect to Keycloak login page
3. **Load Login Page**: Retrieves the Keycloak login form
4. **Submit Credentials**: Posts username/password to Keycloak
5. **Process Callback**: Follows redirect back to backend with authorization code
6. **Exchange Code for Token**: Backend exchanges code for JWT tokens (automatic)
7. **Call Protected Endpoint**: Uses established session to call the target API endpoint

## Authentication Flow

```
Client (Script)          Backend                 Keycloak
     |                      |                        |
     |--1. GET /oauth2/---->|                        |
     |<-- 302 redirect -----|                        |
     |                      |                        |
     |---------------- 2. GET /auth ---------------->|
     |<-------------- Login Page HTML --------------|
     |                      |                        |
     |---------------- 3. POST credentials -------->|
     |<-- 302 redirect with code -------------------|
     |                      |                        |
     |--4. GET /callback -->|                        |
     |   with code          |-- 5. Exchange code -->|
     |                      |<-- Access Token -------|
     |<-- Session Cookie ---|                        |
     |                      |                        |
     |--6. GET /api/xxx --->|                        |
     |   with session       |                        |
     |<-- API Response -----|                        |
```

## Test Results

### ✓ Successful Test: `/api/applications/my`
```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### ✗ User Not Found: `/api/workflow/tasks` and `/api/permits/my`
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "User not found: UserPrincipal{userId=4c77b656-bf81-41f2-aae3-1bf0c042debf, email='user@raptor.local', roles=[USER]}",
  "timestamp": "2025-11-11T18:09:56.759038102"
}
```

**Note**: The 404 error indicates the endpoints are working correctly but the user record doesn't exist in the database. The authentication flow is successful.

## Troubleshooting

### "Failed to obtain tokens from Keycloak"
- Ensure Keycloak container is running: `docker ps | findstr keycloak`
- Check direct grant is enabled: `pwsh -File enable-keycloak-direct-grant.ps1`

### "Failed to complete callback: 404"
- Check if this is an expected business logic error (user not found, resource not found)
- Verify the endpoint path is correct
- Check backend logs: `docker logs rap-backend --tail 50`

### "Response status code does not indicate success: 500"
- Check backend logs for stack traces: `docker logs rap-backend --tail 100`
- Verify database is running and migrations are applied

## Advanced Testing

### Test Multiple Endpoints in Sequence
```powershell
$endpoints = @("/api/workflow/tasks", "/api/permits/my", "/api/applications/my")
foreach ($endpoint in $endpoints) {
    Write-Host "`nTesting: $endpoint" -ForegroundColor Yellow
    pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint $endpoint -QueryParams "page=0&size=10"
}
```

### Capture Response to Variable
```powershell
$output = pwsh -File scripts\local\test-authenticated-endpoint.ps1 -Endpoint "/api/applications/my" -QueryParams "page=0&size=10"
$output | Out-File test-results.txt
```

## Related Scripts

- `test-oidc-flow-advanced.ps1` - Tests OIDC flow using Keycloak Direct Grant
- `enable-keycloak-direct-grant.ps1` - Enables password flow in Keycloak client
