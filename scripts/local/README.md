# Local Development Scripts

This directory contains PowerShell scripts for local development and testing.

## Quick Start After Docker Cleanup

If you've cleared Docker images/containers/volumes, run this single command:

```powershell
.\scripts\local\setup-after-docker-cleanup.ps1
```

This will:
- ✅ Start all services (Keycloak, database, backend)
- ✅ Configure Keycloak client
- ✅ Create test user
- ✅ Set Keycloak frontendUrl
- ✅ Update .env with new client secret
- ✅ Restart backend

## Available Scripts

### Setup & Configuration

#### `setup-after-docker-cleanup.ps1`
**Purpose**: Complete automated setup after Docker cleanup

**Usage**:
```powershell
# Full setup from scratch
.\scripts\local\setup-after-docker-cleanup.ps1

# Reconfigure only (services already running)
.\scripts\local\setup-after-docker-cleanup.ps1 -SkipServiceStart

# Custom test user password
.\scripts\local\setup-after-docker-cleanup.ps1 -TestUserPassword "MyPassword123!"
```

**What it does**:
1. Starts Docker services
2. Waits for Keycloak to be ready
3. Creates Keycloak client and test user
4. Configures frontendUrl
5. Updates .env file
6. Restarts backend

---

#### `configure-keycloak-client.ps1`
**Purpose**: Create/update Keycloak client and test user

**Usage**:
```powershell
.\scripts\local\configure-keycloak-client.ps1
.\scripts\local\configure-keycloak-client.ps1 -UserPassword "CustomPassword123!"
```

**What it does**:
- Creates `raptor-client` OAuth2 client
- Configures redirect URIs
- Generates client secret
- Creates test user `user@raptor.local`
- Assigns USER role

**Output**: Displays client secret that needs to be added to .env

---

#### `configure-keycloak-frontend-url.ps1`
**Purpose**: Set Keycloak's frontendUrl to ensure consistent token issuer

**Usage**:
```powershell
.\scripts\local\configure-keycloak-frontend-url.ps1
```

**What it does**:
- Sets Keycloak realm's frontendUrl to `http://localhost:9090`
- Ensures all tokens have `iss=http://localhost:9090/realms/raptor`

**Why needed**: Solves hostname mismatch issues between browser and backend container

---

### Testing Scripts

#### `test-oidc-flow-advanced.ps1`
**Purpose**: Comprehensive OAuth2 flow testing

**Usage**:
```powershell
.\scripts\local\test-oidc-flow-advanced.ps1
.\scripts\local\test-oidc-flow-advanced.ps1 -ShowDetails
```

**Tests performed**:
1. ✅ Backend /auth/login endpoint
2. ✅ Obtain tokens from Keycloak (Direct Grant)
3. ✅ Retrieve user info from Keycloak
4. ✅ Decode and validate ID Token
5. ✅ Access protected API with Bearer token
6. ✅ Refresh access token

**Output**: Test summary with pass/fail status

---

#### `test-oidc-flow.ps1`
**Purpose**: Basic OAuth2 flow test

**Usage**:
```powershell
.\scripts\local\test-oidc-flow.ps1
```

**Tests**: Simpler version of advanced test, useful for quick validation

---

## Common Workflows

### 1. Fresh Start After Docker Cleanup

```powershell
# One command does everything
.\scripts\local\setup-after-docker-cleanup.ps1

# Then test in browser
# Open http://localhost:4200
# Click Login
# Enter: user@raptor.local / Arif@123456789012
```

### 2. Update Keycloak Configuration Only

```powershell
# If Keycloak lost configuration but services are running
.\scripts\local\setup-after-docker-cleanup.ps1 -SkipServiceStart
```

### 3. Regenerate Client Secret

```powershell
# 1. Run client configuration
.\scripts\local\configure-keycloak-client.ps1

# 2. Copy the displayed client secret

# 3. Update .env file
# Change: OIDC_CLIENT_SECRET=<new-secret>

# 4. Restart backend
docker-compose restart backend
```

### 4. Verify OAuth2 Flow

```powershell
# Run comprehensive tests
.\scripts\local\test-oidc-flow-advanced.ps1

# Should show 5-6 tests passed
```

---

## Configuration Files Modified

These scripts read from and write to:

| File | Purpose | Modified By |
|------|---------|-------------|
| `.env` | Environment variables | `setup-after-docker-cleanup.ps1` |
| `docker-compose.yml` | Service definitions | Manual only |
| Keycloak Database | Client config, users | `configure-keycloak-client.ps1` |
| Keycloak Realm | frontendUrl | `configure-keycloak-frontend-url.ps1` |

---

## Troubleshooting

### Script fails with "Keycloak not accessible"

**Problem**: Keycloak container not ready

**Solution**:
```powershell
# Check if Keycloak is running
docker ps | Select-String keycloak

# Check Keycloak logs
docker logs rap-keycloak --tail 50

# Wait longer for Keycloak startup
Start-Sleep -Seconds 60
.\scripts\local\setup-after-docker-cleanup.ps1 -SkipServiceStart
```

---

### Backend shows "401 Unauthorized" during login

**Problem**: Client secret mismatch

**Solution**:
```powershell
# 1. Get current client secret from Keycloak
.\scripts\local\configure-keycloak-client.ps1

# 2. Copy the secret shown in output

# 3. Update .env file
# OIDC_CLIENT_SECRET=<paste-secret-here>

# 4. Restart backend
docker-compose restart backend
```

---

### Test user can't login

**Problem**: User not created or wrong password

**Solution**:
```powershell
# Recreate user with specific password
.\scripts\local\configure-keycloak-client.ps1 -UserPassword "Arif@123456789012"
```

---

## Script Dependencies

All scripts require:
- ✅ PowerShell 5.1 or higher
- ✅ Docker Desktop running
- ✅ Services accessible on localhost
- ✅ Internet connection (for Docker images)

---

## Environment Variables Used

Scripts read/write these variables in `.env`:

```properties
# OIDC Configuration
OIDC_CLIENT_ID=raptor-client
OIDC_CLIENT_SECRET=<generated-by-script>
OIDC_AUTHORIZATION_URI=http://localhost:9090/realms/raptor/protocol/openid-connect/auth
OIDC_TOKEN_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/token
OIDC_USER_INFO_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/userinfo
OIDC_JWK_SET_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/certs
```

---

## Test Credentials

| Environment | Username | Password | Purpose |
|-------------|----------|----------|---------|
| Local | `user@raptor.local` | `Arif@123456789012` | Test user for OAuth2 |
| Keycloak Admin | `admin` | `admin` | Keycloak management |
| SQL Server | `sa` | `YourStrong@Passw0rd` | Database admin |

---

## Related Documentation

- `../docs/OAUTH2-HOSTNAME-SOLUTION.md` - OAuth2 hostname resolution strategy
- `../docs/LOCAL-DEVELOPMENT.md` - Local development guide
- `../README.md` - Main backend documentation
