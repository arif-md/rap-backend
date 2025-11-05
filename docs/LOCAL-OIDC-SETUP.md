# Local OIDC Testing Setup Guide

## Quick Start - Keycloak (Recommended for Local Development)

### 1. Run Keycloak in Docker

```bash
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

### 2. Configure Keycloak

1. Open **http://localhost:8180** in your browser
2. Login with `admin` / `admin`
3. Create a new realm: **raptor**
4. Create a new client:
   - Client ID: `raptor-backend`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential`
   - Valid Redirect URIs: `http://localhost:8080/auth/callback`
   - Web Origins: `http://localhost:4200`
5. Go to **Credentials** tab and copy the **Client Secret**
6. Create a test user:
   - Username: `testuser`
   - Email: `testuser@example.com`
   - First Name: `Test`
   - Last Name: `User`
   - Set password (disable temporary)

### 3. Update .env File

```bash
# Copy example file
cp .env.example .env

# Edit .env and update:
OIDC_ISSUER_URI=http://localhost:8180/realms/raptor
OIDC_CLIENT_ID=raptor-backend
OIDC_CLIENT_SECRET=<paste-client-secret-from-keycloak>
OIDC_REDIRECT_URI=http://localhost:8080/auth/callback
```

### 4. Start Services

```bash
# Start backend and database only
./dev.ps1 Dev-Start

# OR start all services (frontend + backend + database + process)
./dev.ps1 Dev-Full
```

### 5. Test Authentication Flow

1. Open **http://localhost:4200** (frontend)
2. Click "Sign in or Create an account with OIDC provider"
3. You'll be redirected to Keycloak login page
4. Enter `testuser` / `<password>`
5. After successful auth, you'll be redirected to dashboard
6. Check browser DevTools → Application → Cookies → `jwt` cookie

---

## Alternative: Auth0 (Cloud-based)

### 1. Create Auth0 Account
- Go to https://auth0.com
- Sign up for free account

### 2. Create Application
1. Dashboard → Applications → Create Application
2. Name: `Raptor Backend`
3. Type: `Regular Web Application`
4. Settings:
   - Allowed Callback URLs: `http://localhost:8080/auth/callback`
   - Allowed Logout URLs: `http://localhost:4200`
   - Allowed Web Origins: `http://localhost:4200`
   - Allowed Origins (CORS): `http://localhost:4200`

### 3. Update .env
```bash
OIDC_ISSUER_URI=https://dev-xxxxx.us.auth0.com
OIDC_CLIENT_ID=<your-client-id>
OIDC_CLIENT_SECRET=<your-client-secret>
```

### 4. Create Test User
- Dashboard → User Management → Users → Create User
- Email: `testuser@example.com`
- Password: `<strong-password>`

---

## Troubleshooting

### Issue: "Redirect URI mismatch"
**Solution:** Ensure OIDC provider has exact redirect URI:
```
http://localhost:8080/auth/callback
```

### Issue: "Invalid client credentials"
**Solution:** Verify client secret in `.env` matches OIDC provider

### Issue: "CORS error"
**Solution:** Add `http://localhost:4200` to allowed origins in OIDC provider

### Issue: "JWT token not found in cookie"
**Solution:** Check browser console for errors. Ensure `USE_SECURE_COOKIES=false` in local development

---

## Environment Variables Summary

| Variable | Local Value | Azure Value |
|----------|-------------|-------------|
| `OIDC_ISSUER_URI` | `http://localhost:8180/realms/raptor` | `https://your-oidc-provider.com` |
| `OIDC_CLIENT_ID` | `raptor-backend` | From OIDC provider |
| `OIDC_CLIENT_SECRET` | From Keycloak | From Azure Key Vault |
| `OIDC_REDIRECT_URI` | `http://localhost:8080/auth/callback` | `https://backend.azurecontainerapps.io/auth/callback` |
| `JWT_SECRET` | Base64 test secret | From Azure Key Vault |
| `FRONTEND_URL` | `http://localhost:4200` | `https://frontend.azurecontainerapps.io` |

---

## Next Steps

After successful local testing:

1. **Frontend Implementation** - Login component, auth service, interceptors
2. **Backend Security** - SecurityConfig, JWT service, auth controller
3. **Azure Deployment** - Key Vault secrets, environment variables in Bicep

Refer to `docs/OIDC-IMPLEMENTATION-GUIDE.md` for complete implementation details.
