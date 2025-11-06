# OIDC Authentication - Quick Start Guide

This is a 5-minute quick start to get OIDC authentication running locally.

## Prerequisites

- Docker Desktop installed
- OIDC provider account (Keycloak, Azure AD, or similar)
- PowerShell (Windows) or Bash (Linux/Mac)

## Step 1: Clone and Navigate

```powershell
cd backend
```

## Step 2: Create Environment Configuration

```powershell
# Create .env from template
.\dev.ps1 Setup
```

## Step 3: Configure OIDC Provider

Edit `backend/.env` and set these values:

### For Keycloak (Local or Cloud)

```bash
# OIDC Endpoints (replace YOUR-KEYCLOAK-URL and YOUR-REALM)
OIDC_AUTHORIZATION_ENDPOINT=https://YOUR-KEYCLOAK-URL/realms/YOUR-REALM/protocol/openid-connect/auth
OIDC_TOKEN_ENDPOINT=https://YOUR-KEYCLOAK-URL/realms/YOUR-REALM/protocol/openid-connect/token
OIDC_USER_INFO_ENDPOINT=https://YOUR-KEYCLOAK-URL/realms/YOUR-REALM/protocol/openid-connect/userinfo
OIDC_JWK_SET_URI=https://YOUR-KEYCLOAK-URL/realms/YOUR-REALM/protocol/openid-connect/certs

# Client credentials (from Keycloak client configuration)
OIDC_CLIENT_ID=raptor-app
OIDC_CLIENT_SECRET=your-client-secret-from-keycloak

# JWT Secret (generate random 256-bit secret)
JWT_SECRET=your-random-256-bit-secret-in-base64
```

### For Azure AD (Entra ID)

```bash
# OIDC Endpoints (replace YOUR-TENANT-ID)
OIDC_AUTHORIZATION_ENDPOINT=https://login.microsoftonline.com/YOUR-TENANT-ID/oauth2/v2.0/authorize
OIDC_TOKEN_ENDPOINT=https://login.microsoftonline.com/YOUR-TENANT-ID/oauth2/v2.0/token
OIDC_USER_INFO_ENDPOINT=https://graph.microsoft.com/oidc/userinfo
OIDC_JWK_SET_URI=https://login.microsoftonline.com/YOUR-TENANT-ID/discovery/v2.0/keys

# Client credentials (from Azure AD app registration)
OIDC_CLIENT_ID=your-application-client-id
OIDC_CLIENT_SECRET=your-application-client-secret

# JWT Secret (generate random 256-bit secret)
JWT_SECRET=your-random-256-bit-secret-in-base64
```

## Step 4: Generate JWT Secret

**PowerShell:**
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)
Write-Host "Your JWT Secret: $jwtSecret"
# Copy this value to .env file JWT_SECRET
```

**Bash:**
```bash
openssl rand -base64 32
# Copy output to .env file JWT_SECRET
```

## Step 5: Configure OIDC Provider Redirect URI

In your OIDC provider, add this redirect URI:
```
http://localhost:8080/auth/callback
```

**Keycloak:**
1. Go to Clients → raptor-app → Settings
2. Add to "Valid Redirect URIs": `http://localhost:8080/auth/callback`
3. Save

**Azure AD:**
1. Go to App Registrations → Your App → Authentication
2. Add platform: Web
3. Add redirect URI: `http://localhost:8080/auth/callback`
4. Save

## Step 6: Start Application

```powershell
# Start full stack (frontend + backend + database)
.\dev.ps1 Dev-Full
```

Wait ~30 seconds for containers to start.

## Step 7: Test Authentication

### Test 1: Check Auth Endpoint
```powershell
curl http://localhost:8080/auth/login
```

**Expected Output:**
```json
{
  "authorizationUrl": "https://YOUR-OIDC-PROVIDER/...?client_id=..."
}
```

### Test 2: Full Login Flow

1. Open browser: `http://localhost:4200`
2. Click "Login" (you'll need to implement this button)
3. Or directly visit: Copy the `authorizationUrl` from Test 1
4. Authenticate with OIDC provider
5. Browser redirects to `http://localhost:8080/auth/callback?code=...`
6. Backend issues JWT tokens and redirects to frontend

### Test 3: Check Authenticated User

```powershell
# After logging in, cookies are set
# Test with browser or use cookie from login
curl http://localhost:8080/auth/user -H "Cookie: access_token=YOUR_TOKEN"
```

**Expected Output:**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "fullName": "John Doe",
  "roles": ["USER"]
}
```

## Step 8: Test Admin Features

### Assign Admin Role (via database)

```sql
-- Connect to database: localhost:1433
-- User: sa
-- Password: YourStrong@Passw0rd

-- Find user ID
SELECT id, email FROM users;

-- Assign ADMIN role
INSERT INTO user_roles (user_id, role_id, granted_by)
SELECT user_id, r.id, user_id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'your-email@example.com'
  AND r.name = 'ADMIN';
```

### Test Admin Endpoint

```powershell
curl http://localhost:8080/api/admin/users -H "Cookie: access_token=YOUR_TOKEN"
```

**Expected Output:**
```json
{
  "success": true,
  "total": 1,
  "users": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "fullName": "John Doe",
      "roles": ["USER", "ADMIN"],
      "isActive": true,
      "lastLoginAt": "2025-11-05T10:30:00"
    }
  ]
}
```

## Common Issues

### Issue: "Invalid redirect URI"

**Solution:** Add `http://localhost:8080/auth/callback` to OIDC provider's allowed redirect URIs.

### Issue: "Invalid client credentials"

**Solution:** Verify `OIDC_CLIENT_ID` and `OIDC_CLIENT_SECRET` match OIDC provider configuration.

### Issue: "Database connection failed"

**Solution:** 
```powershell
# Restart database container
.\dev.ps1 Dev-Stop
.\dev.ps1 Dev-Start
```

### Issue: "JWT validation failed"

**Solution:** Ensure `JWT_SECRET` is at least 256 bits (32 bytes in Base64).

## Stopping the Application

```powershell
.\dev.ps1 Dev-Stop
```

## Next Steps

1. **Implement Frontend**
   - Create login button in Angular
   - Implement auth service
   - Add HTTP interceptor for token attachment
   - Add route guards

2. **Test Token Refresh**
   - Wait 15 minutes for access token to expire
   - Test automatic refresh or forced re-auth

3. **Test Logout**
   ```powershell
   curl -X POST http://localhost:8080/auth/logout -H "Cookie: access_token=YOUR_TOKEN"
   ```

4. **Deploy to Azure**
   - See `infra/docs/OIDC-CONFIGURATION.md` for Azure deployment guide

## Configuration Reference

| Environment Variable | Required | Default | Description |
|---------------------|----------|---------|-------------|
| `OIDC_AUTHORIZATION_ENDPOINT` | ✅ Yes | - | OIDC authorization URL |
| `OIDC_TOKEN_ENDPOINT` | ✅ Yes | - | OIDC token endpoint URL |
| `OIDC_USER_INFO_ENDPOINT` | ✅ Yes | - | OIDC user info endpoint URL |
| `OIDC_JWK_SET_URI` | ✅ Yes | - | OIDC JWK set URI |
| `OIDC_CLIENT_ID` | ✅ Yes | - | OIDC client ID |
| `OIDC_CLIENT_SECRET` | ✅ Yes | - | OIDC client secret |
| `JWT_SECRET` | ✅ Yes | - | JWT signing secret (256-bit) |
| `JWT_ISSUER` | No | `raptor-app` | JWT issuer |
| `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES` | No | `15` | Access token TTL |
| `JWT_REFRESH_TOKEN_EXPIRATION_DAYS` | No | `7` | Refresh token TTL |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:4200` | CORS allowed origins |
| `FRONTEND_URL` | No | `http://localhost:4200` | Frontend URL for redirects |

## Useful Commands

```powershell
# View logs
.\dev.ps1 Dev-Logs

# Restart containers
.\dev.ps1 Dev-Stop
.\dev.ps1 Dev-Start

# Connect to database
docker exec -it rap-backend-sqlserver-1 /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P YourStrong@Passw0rd

# View container status
docker ps

# Clear all data and restart
.\dev.ps1 Dev-Stop
docker volume rm rap-backend_sqlserver_data
.\dev.ps1 Dev-Start
```

## Documentation

- **Complete Implementation Summary**: `backend/docs/OIDC-IMPLEMENTATION-SUMMARY.md`
- **Authentication Architecture**: `backend/docs/AUTHENTICATION-ARCHITECTURE.md`
- **Azure Deployment Guide**: `infra/docs/OIDC-CONFIGURATION.md`
- **Local OIDC Setup**: `backend/docs/LOCAL-OIDC-SETUP.md`

---

**Last Updated:** 2025-11-05  
**Need Help?** Check the full documentation or create an issue on GitHub.
