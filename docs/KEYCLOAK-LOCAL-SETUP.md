# Keycloak Local Setup Guide

## Overview

This guide walks you through setting up Keycloak as an OIDC provider for local development with the RAP (Raptor) application.

## Prerequisites

- Docker Desktop installed and running
- RAP backend and frontend repositories cloned
- Port 9090 available (Keycloak on docker-compose)
- Port 8090 used by jBPM Process Service

## Step 1: Start Keycloak Container

### Option A: Using Docker Compose (Recommended)

Keycloak is already configured in `backend/docker-compose.yml` and will start automatically with other services.

**Start all services including Keycloak:**

```powershell
cd backend
.\dev.ps1 Dev-Full
```

This starts:
- SQL Server database (port 1433)
- Keycloak (port 9090)
- Keycloak PostgreSQL database
- Backend Spring Boot (port 8080)
- Frontend Angular (port 4200)
- jBPM Process Service (port 8090)

**Start only Keycloak and its database:**

```powershell
cd backend
docker-compose up -d keycloak keycloak-db
```

### Option B: Using Docker Compose File (Standalone)

If you want Keycloak separate from other services, create a `docker-compose.keycloak.yml` file in the backend directory:

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: rap-keycloak
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HTTP_PORT: 9090
      KC_HOSTNAME_STRICT: "false"
      KC_HOSTNAME_STRICT_HTTPS: "false"
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak
    ports:
      - "9090:9090"
    command:
      - start-dev
      - --http-port=9090
    depends_on:
      - keycloak-db
    networks:
      - rap-network

  keycloak-db:
    image: postgres:15-alpine
    container_name: rap-keycloak-db
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak
    volumes:
      - keycloak-data:/var/lib/postgresql/data
    networks:
      - rap-network

networks:
  rap-network:
    external: true

volumes:
  keycloak-data:
```

Start Keycloak:

```powershell
cd backend
docker-compose -f docker-compose.keycloak.yml up -d
```

### Option C: Using Docker Run (Standalone, No Persistence)

```powershell
docker run -d `
  --name rap-keycloak `
  --network rap-network `
  -p 9090:9090 `
  -e KEYCLOAK_ADMIN=admin `
  -e KEYCLOAK_ADMIN_PASSWORD=admin `
  quay.io/keycloak/keycloak:23.0 `
  start-dev --http-port=9090
```

### Verify Keycloak is Running

Wait about 30-60 seconds for Keycloak to start, then access:

```
http://localhost:9090
```

You should see the Keycloak welcome page.

## Step 2: Access Keycloak Admin Console

1. Navigate to: `http://localhost:9090/admin`
2. Login with:
   - **Username**: `admin`
   - **Password**: `admin`

You should see the Keycloak administration console.

## Step 3: Create a Realm

1. In the top-left corner, click the dropdown that says **"Master"**
2. Click **"Create Realm"**
3. Configure:
   - **Realm name**: `raptor`
   - **Enabled**: `ON`
4. Click **"Create"**

The realm is now created and you'll be taken to the realm settings page.

## Step 4: Create a Client (Application)

### 4.1 Create the Client

1. In the left sidebar, click **"Clients"**
2. Click **"Create client"** button
3. **General Settings** tab:
   - **Client type**: `OpenID Connect`
   - **Client ID**: `raptor-client`
   - Click **"Next"**

### 4.2 Capability Config

4. **Capability config** tab:
   - **Client authentication**: `OFF` (this is a public client using PKCE)
   - **Authorization**: `OFF`
   - **Authentication flow**: Check these boxes:
     - ✅ Standard flow (Authorization Code Flow with PKCE)
     - ✅ Direct access grants
     - ❌ Implicit flow (not recommended)
     - ❌ Service accounts roles
   - Click **"Next"**

**Important**: With **Client authentication OFF**, Keycloak automatically requires PKCE (Proof Key for Code Exchange) for security. This is the recommended approach for applications that cannot safely store secrets.

### 4.3 Login Settings

5. **Login settings** tab:
   - **Root URL**: `http://localhost:4200`
   - **Home URL**: `http://localhost:4200`
   - **Valid redirect URIs**: 
     ```
     http://localhost:8080/auth/callback
     http://localhost:8080/auth/callback/*
     ```
   - **Valid post logout redirect URIs**: 
     ```
     http://localhost:4200
     http://localhost:4200/*
     ```
   - **Web origins**: `http://localhost:8080`
   - Click **"Save"**

**Note**: Since we're using PKCE (Client authentication OFF), you don't need to copy a client secret. The backend Spring Security OAuth2 client will automatically generate and use PKCE code verifiers.

## Step 5: Configure Token Claims (Role Mapping)

### 5.1 Add Realm Roles Mapper

To include user roles in the JWT token, you need to add a role mapper:

1. In the left sidebar, click **"Clients"**
2. Click on **"raptor-client"**
3. Click the **"Client scopes"** tab
4. Under **"Assigned client scopes"**, click on **"raptor-client-dedicated"**
   - **Note**: If you don't see this, go to **Clients** → **raptor-client** → **Mappers** tab instead (skip to step 6)
5. Click the **"Mappers"** tab

### 5.2 Create Realm Roles Mapper

6. Click **"Add mapper"** → **"By configuration"**
7. Select **"User Realm Role"**
8. Configure:
   - **Name**: `realm-roles`
   - **Token Claim Name**: `roles`
   - **Claim JSON Type**: `String`
   - **Add to ID token**: `ON`
   - **Add to access token**: `ON`
   - **Add to userinfo**: `ON`
9. Click **"Save"**

### 5.3 Add Custom User Attribute Mappers (Optional)

If you want to include custom user attributes like full name:

1. In **"raptor-client-dedicated"** (or **raptor-client** → **Mappers**)
2. Click **"Add mapper"** → **"By configuration"**
3. Select **"User Attribute"**

#### Mapper 1: Full Name (Optional)

- **Name**: `full-name`
- **User Attribute**: `fullName`
- **Token Claim Name**: `name`
- **Claim JSON Type**: `String`
- **Add to ID token**: `ON`
- **Add to access token**: `ON`
- **Add to userinfo**: `ON`
- Click **"Save"**

#### Mapper 2: Email

- Email should already be included by default
- Verify in **"Mappers"** tab that `email` mapper exists

## Step 6: Create Test Users

### 6.1 Create Admin User

1. In the left sidebar, click **"Users"**
2. Click **"Add user"**
3. Configure:
   - **Username**: `admin@raptor.local`
   - **Email**: `admin@raptor.local`
   - **Email verified**: `ON`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Enabled**: `ON`
4. Click **"Create"**

### 6.2 Set Admin Password

5. Click the **"Credentials"** tab
6. Click **"Set password"**
7. Configure:
   - **Password**: `admin123`
   - **Password confirmation**: `admin123`
   - **Temporary**: `OFF` (so user doesn't have to change on first login)
8. Click **"Save"**
9. Confirm by clicking **"Save password"**

### 6.3 Add Admin Role

10. Click the **"Attributes"** tab
11. Add a custom attribute:
    - **Key**: `roles`
    - **Value**: `ADMIN`
12. Click **"Add"**
13. Click **"Save"**

### 6.4 Create Regular User (Optional)

Repeat steps 6.1-6.2 with:
- **Username**: `user@raptor.local`
- **Email**: `user@raptor.local`
- **First name**: `Regular`
- **Last name**: `User`
- **Password**: `user123`
- **Attributes**: Skip (or add `roles: USER`)

## Step 7: Create Realm Roles (Better Approach)

Instead of custom attributes, use Keycloak roles:

### 7.1 Create Roles

1. In the left sidebar, click **"Realm roles"**
2. Click **"Create role"**
3. Create **ADMIN** role:
   - **Role name**: `ADMIN`
   - **Description**: `Administrator role`
   - Click **"Save"**
4. Repeat for **USER** role:
   - **Role name**: `USER`
   - **Description**: `Regular user role`
   - Click **"Save"**

### 7.2 Assign Roles to Users

1. Click **"Users"** in sidebar
2. Click on `admin@raptor.local`
3. Click **"Role mapping"** tab
4. Click **"Assign role"**
5. Check **"ADMIN"** and **"USER"**
6. Click **"Assign"**

Repeat for regular user (assign only **"USER"** role).

### 7.3 Add Roles to Token (Configure Mapper)

The roles won't appear in tokens unless you add a mapper. There are two ways to access the dedicated client scope:

#### Method A: Through Client Scopes (Recommended)

1. Click **"Clients"** in sidebar
2. Click on **"raptor-client"**
3. Click **"Client scopes"** tab (top navigation, not sidebar)
4. Under **"Assigned client scopes"**, find and click **"raptor-client-dedicated"**
5. Click **"Mappers"** tab
6. Click **"Add mapper"** → **"By configuration"**
7. Select **"User Realm Role"**

#### Method B: Direct on Client (If Method A doesn't work)

1. Click **"Clients"** in sidebar
2. Click on **"raptor-client"**
3. Click **"Mappers"** tab (top navigation)
4. Click **"Add mapper"** → **"By configuration"**
5. Select **"User Realm Role"**

#### Configure the Mapper (Both Methods)

Configure the mapper with these settings:
- **Name**: `realm-roles`
- **Multivalued**: `ON`
- **Token Claim Name**: `roles`
- **Claim JSON Type**: `String`
- **Add to ID token**: `ON`
- **Add to access token**: `ON`
- **Add to userinfo**: `ON`
- Click **"Save"**

**Verification**: After saving, the roles will be included in JWT tokens under the `roles` claim as an array of strings.

## Step 8: Configure Backend Environment

### 8.1 Update backend/.env

If you ran `.\dev.ps1 Setup`, you should have a `.env` file. Update it with Keycloak settings:

```bash
# Database (from .env.example)
AZURE_SQL_CONNECTIONSTRING=jdbc:sqlserver://localhost:1433;databaseName=raptor;user=sa;password=YourStrong@Passw0rd;encrypt=true;trustServerCertificate=true

# OIDC Configuration
OIDC_PROVIDER_ISSUER_URI=http://localhost:9090/realms/raptor
OIDC_CLIENT_ID=raptor-client
OIDC_CLIENT_SECRET=xK8Zv2pQmR5tYwA3sD6fG9hJ1kL4nM7oP0qT2uV5x
FRONTEND_URL=http://localhost:4200

# Keycloak Configuration (if using docker-compose)
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak

# Logging (optional)
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_BACKEND=DEBUG
```

**Note**: With PKCE authentication, you don't need `OIDC_CLIENT_SECRET`. The backend automatically generates code verifiers for each authentication request.

### 8.2 Verify application.properties (already configured)

The backend's `src/main/resources/application.properties` should already have OIDC configuration with PKCE enabled.

**Note**: The backend uses `application.properties` (not `.yml`). OIDC settings are configured via environment variables in `.env` and `docker-compose.yml`:

- `OIDC_PROVIDER_ISSUER_URI` - Keycloak issuer URL (optional, if using explicit endpoints)
- `OIDC_CLIENT_ID` - Client identifier
- **NO CLIENT SECRET NEEDED** - PKCE uses dynamically generated code verifiers
- `FRONTEND_URL` - Allowed callback origin

The Spring Boot application reads these environment variables at startup and automatically uses PKCE when `client-authentication-method=none` is configured.

## Step 9: Restart the Backend

Since you updated the configuration, restart the backend to apply changes:

### Using PowerShell dev.ps1

```powershell
cd backend
.\dev.ps1 Dev-Stop
.\dev.ps1 Dev-Start
```

Or restart just the backend container:

```powershell
cd backend
docker-compose restart backend
```

This ensures:
- SQL Server container (port 1433)
- Backend Spring Boot app (port 8080)

### Verify Backend is Running

Check the logs for:

```
Started BackendApplication in X.XXX seconds
```

Test the auth endpoint:

```powershell
curl http://localhost:8080/auth/login
```

Expected response (JSON with `authorizationUrl`):

```json
{
  "authorizationUrl": "http://localhost:9090/realms/raptor/protocol/openid-connect/auth?response_type=code&client_id=raptor-client&scope=openid%20profile%20email&state=..."
}
```

## Step 10: Start the Frontend

### 10.1 Install Dependencies (first time only)

```powershell
cd ..\frontend
npm install
```

### 10.2 Start Angular Dev Server

```powershell
npm start
```

Or use the VS Code task: `Ctrl+Shift+B` → "Run Angular Dev Server"

### 10.3 Verify Frontend

Navigate to: `http://localhost:4200`

You should see the RAP landing page.

## Step 11: Test OIDC Authentication Flow

### 11.1 Access Login Page

1. Click **"Login"** or navigate to `http://localhost:4200/login`
2. You should see a button: **"Sign in with OIDC Provider"**

### 11.2 Initiate Login

3. Click the **"Sign in with OIDC Provider"** button
4. Browser redirects to Keycloak login page:
   ```
   http://localhost:9090/realms/raptor/protocol/openid-connect/auth?...
   ```

### 11.3 Login with Test User

5. Enter credentials:
   - **Username or email**: `admin@raptor.local`
   - **Password**: `admin123`
6. Click **"Sign In"**

### 11.4 Verify Callback

7. Keycloak redirects to: `http://localhost:8080/auth/callback?code=...`
8. Backend exchanges code for tokens, sets cookies
9. Backend redirects to: `http://localhost:4200/auth-callback`
10. Frontend shows "Authenticating..." spinner
11. Frontend redirects to dashboard or home page

### 11.5 Verify Authentication State

12. Check the header navigation:
    - User menu should show "Admin User" (or first/last name)
    - Dropdown should show email: `admin@raptor.local`
    - **Admin** menu item should be visible (because user has ADMIN role)
    - **Logout** option should be present

### 11.6 Check Browser Developer Tools

13. Open DevTools (F12) → **Application** → **Cookies** → `http://localhost:8080`
14. Verify cookies exist:
    - `access_token` (httpOnly)
    - `refresh_token` (httpOnly)

### 11.7 Test Protected API Endpoint

```powershell
# This should work because you're authenticated
curl http://localhost:8080/api/applications `
  -H "Cookie: access_token=...; refresh_token=..." `
  --include
```

Or just navigate in the browser (cookies sent automatically):
```
http://localhost:8080/api/applications
```

### 11.8 Test Logout

15. Click user dropdown → **Logout**
16. Browser redirects to `/login`
17. Cookies cleared
18. Session ended

## Step 12: Test Token Refresh (Optional)

### Wait for Access Token Expiry

1. Access token expires in 15 minutes (default)
2. Make an API call after 15 minutes
3. Backend returns 401
4. AuthInterceptor automatically calls `/auth/refresh`
5. Backend uses refresh token to get new access token
6. Request retries with new token
7. User stays logged in (transparent refresh)

### Watch Console Logs

In browser DevTools → Console, you should see:
```
Token refresh successful
```

## Troubleshooting

### Issue: "Invalid redirect_uri"

**Cause**: Keycloak client redirect URIs don't match backend URL

**Fix**:
1. Go to Keycloak Admin → Clients → `raptor-client`
2. Verify **Valid redirect URIs** includes:
   ```
   http://localhost:8080/auth/callback
   http://localhost:8080/auth/callback/*
   ```
3. Click **Save**

### Issue: "PKCE validation failed" or "Code verifier required"

**Cause**: Keycloak client is still configured for confidential authentication (Client authentication ON) instead of public client with PKCE

**Fix**:
1. Go to Keycloak Admin Console
2. Navigate to **Clients** → **raptor-client** → **Settings** tab
3. Set **Client authentication** to `OFF`
4. Click **Save**
5. Verify **Capability config** shows:
   - Client authentication: `OFF`
   - Standard flow enabled: `ON`

### Issue: "Client authentication failed"

**Cause**: Backend is still configured to send client secret, or Keycloak client still requires authentication

**Fix**:
1. Verify `OIDC_CLIENT_ID=raptor-client` in `.env`
2. **Remove** `OIDC_CLIENT_SECRET` from `.env` (PKCE doesn't use secrets)
3. Verify `application.properties` has:
   ```properties
   spring.security.oauth2.client.registration.oidc-provider.client-authentication-method=none
   ```
4. Verify Keycloak client has **Client authentication = OFF**
5. Restart backend: `.\dev.ps1 Dev-Stop` then `.\dev.ps1 Dev-Start`

### Issue: Database initialization failed (SQL Server timeout)

**Cause**: SQL Server took longer than expected to start, init script timed out

**Symptoms**:
- Backend logs show: `Cannot open database "raptordb" requested by the login`
- Database container logs show: `Login timeout expired`

**Fix**:
The entrypoint script now includes retry logic and better error messages. If initialization still fails:

1. **Check database logs**:
   ```powershell
   docker logs rap-database | Select-String -Pattern "ERROR|Initialization|Database created"
   ```

2. **Recreate database container**:
   ```powershell
   docker-compose down database
   docker volume rm backend_sqlserver-data
   docker-compose up -d database
   ```

3. **Manual database creation** (if init script continues to fail):
   ```powershell
   docker exec rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourStrong@Passw0rd' -C -Q "CREATE DATABASE raptordb"
   docker-compose restart backend
   ```

4. **Verify database exists**:
   ```powershell
   docker exec rap-database /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'YourStrong@Passw0rd' -C -Q "SELECT name FROM sys.databases WHERE name = 'raptordb'"
   ```

### Issue: Can't find "raptor-client-dedicated" in Client Scopes

**Cause**: In Keycloak 23.0+, client-dedicated scopes may not appear in the global Client Scopes list

**Fix**: Access the dedicated scope through the client itself:
1. Go to **Clients** → **raptor-client** → **Client scopes** tab
2. Click on **"raptor-client-dedicated"** under "Assigned client scopes"
3. OR add mappers directly: **Clients** → **raptor-client** → **Mappers** tab

### Issue: "CORS error"

**Cause**: Backend CORS not allowing frontend origin

**Fix**:
1. Verify `FRONTEND_URL=http://localhost:4200` in `.env`
2. CORS is configured via environment variables (no need to modify `application.properties`)
3. Restart backend: `docker-compose restart backend`

### Issue: "User has no roles"

**Cause**: Role mapper not configured or roles not assigned

**Fix**:
1. Verify realm roles exist (ADMIN, USER)
2. Verify user has roles assigned:
   - Users → `admin@raptor.local` → Role mapping
   - Should show ADMIN and USER
3. Verify role mapper exists:
   - Client scopes → `raptor-client-dedicated` → Mappers
   - Should see `realm-roles` mapper with claim name `roles`

### Issue: "Cookies not being set"

**Cause**: SameSite or Secure cookie settings

**Fix**:
1. Backend `CookieUtil.java` should set:
   ```java
   cookie.setSecure(false); // For HTTP (local dev)
   cookie.setSameSite("Lax");
   ```
2. Clear browser cookies and try again
3. Check browser DevTools → Network → callback request → Response Headers:
   ```
   Set-Cookie: access_token=...; Path=/; HttpOnly; SameSite=Lax
   Set-Cookie: refresh_token=...; Path=/; HttpOnly; SameSite=Lax
   ```

### Issue: "401 after page refresh"

**Cause**: Cookies expired or not sent, localStorage cleared

**Fix**:
1. Check cookie expiry in DevTools
2. Verify `withCredentials: true` in frontend HTTP requests
3. Check `AuthenticationService` constructor loads from localStorage:
   ```typescript
   const storedUser = localStorage.getItem('currentUser');
   this.currentUserSubject = new BehaviorSubject<User | null>(
     storedUser ? JSON.parse(storedUser) : null
   );
   ```

## Advanced Configuration

### Custom Token Lifespan

1. Keycloak Admin → Realm settings → Tokens tab
2. Adjust:
   - **Access Token Lifespan**: `15 minutes` (default, increase for testing)
   - **SSO Session Idle**: `30 minutes`
   - **SSO Session Max**: `10 hours`
   - **Refresh Token Max Reuse**: `0` (no reuse, more secure)

### Email Verification

1. Keycloak Admin → Realm settings → Login tab
2. Enable:
   - **User registration**: `ON`
   - **Verify email**: `ON`
   - **Forgot password**: `ON`
3. Configure SMTP:
   - Realm settings → Email tab
   - Configure your SMTP server (Gmail, SendGrid, etc.)

### Multi-Factor Authentication

1. Keycloak Admin → Authentication
2. Click **"Flows"** tab
3. Duplicate **"browser"** flow
4. Add **"OTP Form"** execution
5. Set to **"Required"**
6. Bind to browser flow

### Custom Themes

1. Create custom theme in Keycloak themes directory
2. Keycloak Admin → Realm settings → Themes tab
3. Select your custom theme for:
   - **Login theme**
   - **Account theme**
   - **Email theme**

## Summary Checklist

Before starting development, verify:

- ✅ Keycloak running on `http://localhost:9090`
- ✅ Realm `raptor` created
- ✅ Client `raptor-client` configured with correct redirect URIs
- ✅ Roles `ADMIN` and `USER` created
- ✅ Role mapper added to client scope
- ✅ Test users created with roles assigned
- ✅ Backend `.env` has correct Keycloak credentials
- ✅ Backend running on `http://localhost:8080`
- ✅ Frontend running on `http://localhost:4200`
- ✅ Login flow works: login → Keycloak → callback → authenticated
- ✅ User menu shows name and email
- ✅ Admin menu visible for admin users
- ✅ Logout works and clears session

## Next Steps

- **Production Setup**: See `backend/docs/OIDC-CONFIGURATION.md` for production Keycloak setup
- **Azure AD Setup**: See same guide for Azure AD OIDC configuration
- **CI/CD Integration**: Configure secrets in GitHub Actions for automated deployments
- **Monitoring**: Enable Keycloak event logging and integrate with monitoring tools

## References

- Keycloak Documentation: https://www.keycloak.org/documentation
- Spring Security OAuth2: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
- OpenID Connect Specification: https://openid.net/specs/openid-connect-core-1_0.html
- Backend OIDC Docs: `backend/docs/OIDC-IMPLEMENTATION-SUMMARY.md`
- Frontend OIDC Docs: `frontend/FRONTEND-OIDC-IMPLEMENTATION.md`
