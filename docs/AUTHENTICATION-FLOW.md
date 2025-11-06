# Authentication Flow Configuration

## Current Implementation

### Token Strategy
- **Access Token**: 15 minutes (JWT stored in httpOnly cookie)
- **Refresh Token**: 7 days (Random UUID stored in httpOnly cookie + database)

### Security Features
1. **Revocation Check on Every Request**: 
   - Every authenticated request checks the `revoked_tokens` table
   - Admins can instantly invalidate compromised tokens
   - Trade-off: Database hit on every request (consider Redis cache for production)

2. **OIDC Re-authentication on Expiry**:
   - When access token expires, users are redirected to OIDC provider
   - OIDC provider can issue new token silently (if session active) or require login
   - Refresh tokens are available but discouraged for web apps

## Authentication Endpoints

### GET /auth/login
- Redirects to OIDC provider for authentication
- Returns OAuth2 authorization URL

### GET /auth/callback
- Handles OIDC callback after successful authentication
- Creates/updates user in database
- Generates JWT access token + refresh token
- Sets httpOnly cookies
- Returns user info and redirect URL to frontend

### POST /auth/refresh
- **Current Behavior**: Silent token refresh (not recommended for your use case)
- **Recommended Behavior**: Uncomment OPTION 1 in code to force OIDC re-auth
- Returns `requiresReauth: true` to tell frontend to redirect to `/auth/login`

### POST /auth/logout
- Revokes access token (adds to blacklist)
- Revokes refresh token (marks as revoked in DB)
- Clears httpOnly cookies
- User must re-authenticate with OIDC

### GET /auth/user
- Returns current authenticated user info from JWT
- Validates token and checks revocation list

### GET /auth/check
- Checks session validity without issuing new tokens
- Returns authentication status:
  - `authenticated: true` → Access token valid, continue
  - `requiresReauth: true` → Access token expired, redirect to OIDC
  - `authenticated: false` → No valid session, redirect to login

## Frontend Integration Guide

### Recommended Flow for Your Requirements

```typescript
// 1. On app load, check session
const sessionStatus = await fetch('/auth/check');
if (!sessionStatus.authenticated) {
  window.location.href = '/auth/login'; // Redirect to OIDC
}

// 2. On API 401 error, redirect to OIDC (don't call /auth/refresh)
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      window.location.href = '/auth/login'; // Force OIDC re-auth
    }
    return Promise.reject(error);
  }
);

// 3. Optional: Proactive session check before expiry
setInterval(async () => {
  const status = await fetch('/auth/check');
  if (status.requiresReauth) {
    // Show "Session expiring" modal with countdown
    showSessionModal(() => {
      window.location.href = '/auth/login'; // User clicks "Continue"
    });
  }
}, 13 * 60 * 1000); // Check at 13 minutes (before 15-min expiry)
```

### Alternative: Silent Refresh (Less Secure)

If you want to use refresh tokens for silent session extension:

```typescript
// Call /auth/refresh before access token expires
setInterval(async () => {
  const response = await fetch('/auth/refresh', { method: 'POST' });
  if (response.status === 401 && response.requiresReauth) {
    window.location.href = '/auth/login'; // Fallback to OIDC
  }
}, 13 * 60 * 1000);
```

**Not recommended because:**
- Users never go back to OIDC provider (sessions can last 7 days)
- If OIDC provider revokes access, your app doesn't know
- Less secure for compliance requirements (e.g., banking, healthcare)

## OIDC Provider Behavior

### Silent Re-authentication
If your OIDC provider has an active session (SSO cookie), it will:
1. User redirected to `/auth/login`
2. OIDC provider detects active session
3. Issues new OIDC token automatically (no password prompt)
4. Redirects back to `/auth/callback`
5. Backend generates new JWT access token
6. User returned to frontend (seamless UX)

### Forced Re-authentication
If OIDC session expired or you want to force login:
1. Add `prompt=login` to OIDC authorization URL
2. User must re-enter credentials
3. More secure for sensitive operations

## Configuration Options

### Option 1: Force OIDC Re-auth (Recommended for Your Use Case)

**In `AuthController.java` → `refreshToken()` method:**
```java
// Uncomment OPTION 1 block to disable silent refresh
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("requiresReauth", true);
errorResponse.put("loginUrl", "/auth/login");
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
```

**Frontend behavior:**
- Access token expires → `/auth/check` returns `requiresReauth: true`
- Frontend redirects to `/auth/login` → OIDC provider
- OIDC issues new token (silently if session active)
- User back in app with new 15-min session

### Option 2: Silent Refresh (Current Default)

Keep current code as-is. Frontend calls `/auth/refresh` to extend session without OIDC interaction.

### Option 3: Hybrid (Best of Both Worlds)

- Normal expiry: Silent refresh with `/auth/refresh`
- After 24 hours OR suspicious activity: Force OIDC re-auth
- Implement by checking `last_login_at` timestamp or session flags

## Performance Optimization

### Current: DB Check on Every Request
```java
// In JwtTokenService.validateAccessToken()
String jti = jwtTokenUtil.getJtiFromToken(jwt);
return !revokedTokenHandler.isRevoked(jti); // DB query
```

### Recommended: Add Redis Cache
```java
// Check cache first, fallback to DB
boolean isRevoked = redisTemplate.hasKey("revoked:" + jti) 
    || revokedTokenHandler.isRevoked(jti);
```

**Benefits:**
- 10-100x faster token validation
- Reduce database load
- Cache expiration matches token expiration (15 min)

## Security Considerations

1. **HttpOnly Cookies**: ✅ Prevents XSS attacks
2. **Secure Flag**: Set to `true` in production (HTTPS only)
3. **SameSite**: Set to `Strict` or `Lax` for CSRF protection
4. **Token Rotation**: Each refresh generates new access token
5. **Revocation List**: Cleanup job removes expired entries (see scheduled task)

## Migration Path

If you want to switch from silent refresh → OIDC re-auth:

1. Update `AuthController.refreshToken()` to return `requiresReauth: true`
2. Update frontend to handle 401 with redirect to `/auth/login`
3. Test with OIDC provider silent re-auth flow
4. Monitor user experience (should be seamless if OIDC session active)
5. Optionally remove refresh token tables/logic (or keep for mobile apps)
