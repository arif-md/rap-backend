# OIDC Testing Guide

## Quick Reference

### Test Scripts Available

| Script | Purpose | Tests | Browser Required? |
|--------|---------|-------|-------------------|
| `test-oidc-flow.ps1` | Infrastructure validation | 6 automated tests | No (but provides manual instructions) |
| `test-oidc-flow-advanced.ps1` | Token acquisition test | Token/JWT validation | No (attempts programmatic auth) |

---

## Running Tests

### 1. Infrastructure Test (Recommended First)

```powershell
cd backend
.\test-oidc-flow.ps1
```

**What it tests:**
- ✓ Backend `/auth/login` endpoint
- ✓ OAuth2 authorization redirect to Keycloak
- ✓ Keycloak OIDC discovery endpoints
- ✓ Backend-to-Keycloak Docker connectivity
- ✓ Split-horizon DNS configuration
- ✓ Session cookie handling

**Expected output:**
```
Total Tests: 6
Passed: 6
Failed: 0
✓ All automated tests passed!
```

### 2. Advanced Token Test (Optional)

```powershell
.\test-oidc-flow-advanced.ps1
```

**Note:** Will fail if Direct Grant is disabled (which is good security practice). The script demonstrates what would happen in a programmatic token acquisition scenario.

---

## Manual Browser Testing

### Complete End-to-End Flow

1. **Start services:**
   ```powershell
   cd backend
   .\dev.ps1 Dev-Full  # Starts all services including frontend
   ```

2. **Open browser:**
   - Navigate to: `http://localhost:4200`

3. **Initiate login:**
   - Click: "Login with OIDC Provider" button

4. **Authenticate with Keycloak:**
   - You'll be redirected to: `http://localhost:9090/realms/raptor/protocol/openid-connect/auth`
   - Username: `admin`
   - Password: `admin`

5. **Verify successful authentication:**
   - Browser redirects to: `http://localhost:4200/dashboard`
   - Cookies set: `access_token`, `refresh_token` (HttpOnly)
   - Check DevTools → Application → Cookies

6. **Test API access:**
   ```powershell
   # With browser cookies active, API calls should work
   curl http://localhost:8080/api/applications
   ```

---

## Browser DevTools Verification

Open **Network tab** in DevTools and verify the following sequence:

| # | Request | Expected Response | Notes |
|---|---------|-------------------|-------|
| 1 | `GET /auth/login` | 200 OK | Returns `{ "authorizationUrl": "/oauth2/..." }` |
| 2 | `GET /oauth2/authorization/oidc-provider` | 302 Redirect | Sets `JSESSIONID` cookie, redirects to Keycloak |
| 3 | `GET http://localhost:9090/realms/raptor/...auth` | 200 OK | Keycloak login page |
| 4 | `POST http://localhost:9090/realms/raptor/...authenticate` | 302 Redirect | After form submission |
| 5 | `GET /auth/callback?code=...&state=...` | 302 Redirect | Backend validates state, exchanges code for tokens |
| 6 | Final redirect | 200 OK | Dashboard loads, JWT cookies set |

### Expected Cookies After Login

| Cookie Name | Type | HttpOnly | SameSite | Purpose |
|-------------|------|----------|----------|---------|
| `access_token` | JWT | Yes | Lax | API authentication |
| `refresh_token` | JWT | Yes | Lax | Token renewal |
| `JSESSIONID` | Session | Yes | Lax | OAuth2 state (temporary, expires after login) |

**Note:** `JSESSIONID` is only needed during the OAuth2 flow (~30-60 seconds). After JWT tokens are issued, API calls use the JWT cookies (stateless).

---

## Troubleshooting

### Issue: `/auth/callback` returns error

**Symptom:** Error page showing "Cannot invoke OidcUser.getSubject() because oidcUser is null"

**Cause:** Session was not preserved between authorization request and callback

**Solutions:**
1. Verify `SessionCreationPolicy.IF_REQUIRED` is set in SecurityConfig ✓ (Applied)
2. Check that cookies are enabled in browser
3. Ensure no proxy/load balancer is stripping cookies
4. For multi-instance deployments: Enable sticky sessions OR configure Redis

### Issue: Redirect loops

**Symptom:** Browser keeps redirecting between backend and Keycloak

**Possible causes:**
- Keycloak client configuration incorrect
- Redirect URI mismatch
- CORS issues

**Check:**
```powershell
# Verify Keycloak client configuration
curl http://localhost:9090/admin/realms/raptor/clients
```

### Issue: 401 Unauthorized on API calls

**Symptom:** API returns 401 even after successful login

**Check:**
1. JWT cookies are set in browser
2. Cookie domain matches (localhost:4200 → localhost:8080 = same domain ✓)
3. JWT token hasn't expired
4. Backend JWT validation is working

**Debug:**
```powershell
# Check backend logs for JWT validation errors
docker logs rap-backend --tail 50 | Select-String -Pattern "JWT|Token|Authentication"
```

---

## Session vs Stateless Architecture

### OAuth2 Login Flow (Session-Based - Temporary)

```
Duration: ~30-60 seconds
Storage: In-memory sessions (or Redis in production)
Purpose: Store OAuth2 state parameter for CSRF protection
```

**Timeline:**
1. User clicks login → Session created (stores state)
2. Redirect to Keycloak → Session preserved
3. Callback from Keycloak → Session validates state
4. JWT tokens issued → Session can be destroyed

### API Access (Stateless - Permanent)

```
Duration: Until token expiry
Storage: JWT in HttpOnly cookies
Purpose: Authenticate all API requests
```

**Characteristics:**
- No server-side session lookup
- JWT contains all user info (self-contained)
- Scales horizontally without session replication
- True RESTful stateless architecture

---

## Production Deployment (Multi-Instance)

### Option 1: Sticky Sessions (Simplest)

**Azure Container Apps:**
```yaml
sessionAffinity:
  affinity: sticky
```

**Pros:**
- No code changes
- Works immediately

**Cons:**
- Uneven load distribution during login

### Option 2: Redis Session Store (Recommended)

**Add dependencies:**
```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

**Configure:**
```properties
spring.session.store-type=redis
spring.session.timeout=300s
spring.data.redis.host=${REDIS_HOST}
```

**Pros:**
- True high availability
- Works with any number of instances

**Cons:**
- Requires Redis deployment

See: `backend/docs/SESSION-VS-STATELESS-ARCHITECTURE.md` for detailed implementation guide.

---

## Quick Commands

```powershell
# Run all services
cd backend
.\dev.ps1 Dev-Full

# Run infrastructure tests
.\test-oidc-flow.ps1

# Check backend logs
docker logs rap-backend --tail 50

# Check if backend is healthy
curl http://localhost:8080/actuator/health

# Test login endpoint
curl http://localhost:8080/auth/login | ConvertFrom-Json

# View running containers
docker-compose ps
```

---

## What Changed (Session Fix)

**Before:**
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```
- ❌ No sessions created
- ❌ OAuth2 state couldn't be stored
- ❌ Callback failed with null oidcUser

**After:**
```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
```
- ✓ Sessions created only for OAuth2 flow
- ✓ State parameter stored and validated
- ✓ Callback succeeds with authenticated user
- ✓ JWT tokens issued
- ✓ API calls remain stateless

---

## Next Steps

1. ✅ Run `.\test-oidc-flow.ps1` to verify infrastructure
2. ✅ Test complete flow in browser (http://localhost:4200)
3. ⏭️ Verify JWT tokens work for API calls
4. ⏭️ Test token refresh flow
5. ⏭️ Plan production deployment strategy (sticky sessions vs Redis)

---

## Resources

- **OIDC Configuration:** `infra/docs/OIDC-CONFIGURATION.md`
- **Session Architecture:** `backend/docs/SESSION-VS-STATELESS-ARCHITECTURE.md`
- **Keycloak Setup:** `backend/docs/KEYCLOAK-LOCAL-SETUP.md`
- **Test Scripts:** `backend/test-oidc-flow.ps1`, `backend/test-oidc-flow-advanced.ps1`
