# OIDC Authentication Architecture Guide

## Table of Contents
1. [Why Refresh Tokens Are Needed](#why-refresh-tokens-are-needed)
2. [How Access Tokens and Refresh Tokens Work Together](#how-tokens-work-together)
3. [Automatic Session Extension (Proactive Token Refresh)](#proactive-token-refresh)
4. [Silent Refresh vs Forced OIDC Re-authentication](#silent-refresh-vs-forced-oidc)
5. [Future Performance Improvement with Redis Cache](#redis-cache-optimization)
6. [Admin Features: Active User Monitoring](#admin-active-users)

---

## 1. Why Refresh Tokens Are Needed {#why-refresh-tokens-are-needed}

### The Problem: Balancing Security and User Experience

**If we only used access tokens:**
- ❌ Short expiry (15 min) → Users must login every 15 minutes (terrible UX)
- ❌ Long expiry (7 days) → Compromised tokens valid for 7 days (terrible security)

### The Solution: Two-Token Strategy

**Access Token (Short-lived - 15 minutes)**
- Sent with **every API request** (high exposure)
- Stored in httpOnly cookie
- Contains user ID, email, roles
- Validated on every request (signature + blacklist check)
- If compromised: Limited damage (15 min window)

**Refresh Token (Long-lived - 7 days)**
- Sent to **one endpoint only** (`/auth/refresh`)
- Stored in httpOnly cookie + database (hashed)
- Used to obtain new access tokens
- If compromised: Can be revoked from database

### Security Benefits

1. **Reduced Attack Surface**
   - Access token: Transmitted 100s of times → Higher interception risk
   - Refresh token: Transmitted once every 15 min → Lower exposure

2. **Defense in Depth**
   ```
   Attacker steals access token  → Expires in 15 min ✓
   Attacker steals refresh token → Admin can revoke ✓
   Attacker steals both          → Refresh token is httpOnly (harder to steal) ✓
   ```

3. **Granular Revocation**
   - Logout → Revoke refresh token → All future access tokens denied
   - No need to check blacklist on EVERY request (just when refreshing)

### Industry Standard

OAuth2 RFC 6749 recommends this pattern:
- **Access token**: 5-30 minutes (most use 15 min)
- **Refresh token**: 1-90 days (most use 7-30 days)
- **Result**: Balance of security and UX

---

## 2. How Access Tokens and Refresh Tokens Work Together {#how-tokens-work-together}

### Authentication Flow (First Login)

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│ Frontend │                 │  Backend │                 │   OIDC   │
│          │                 │          │                 │ Provider │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │  1. User clicks "Login"    │                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │  2. Redirect to OIDC       │  3. Redirect to OIDC       │
     │<───────────────────────────┤───────────────────────────>│
     │                            │                            │
     │  4. User enters credentials│                            │
     │────────────────────────────┼───────────────────────────>│
     │                            │                            │
     │  5. OIDC callback + code   │  6. Callback with auth code│
     │<───────────────────────────┼────────────────────────────│
     │                            │<───────────────────────────┤
     │                            │                            │
     │                            │  7. Exchange code for OIDC │
     │                            │     token (backend-to-backend)
     │                            │───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  8. OIDC token returned    │
     │                            │                            │
     │                            │  9. Create/update user in DB
     │                            │  10. Generate JWT access token (15 min)
     │                            │  11. Generate refresh token (7 days)
     │                            │  12. Store refresh token in DB (hashed)
     │                            │                            │
     │  13. Set cookies & return  │                            │
     │      - access_token        │                            │
     │      - refresh_token       │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  14. Redirect to dashboard │                            │
```

### Normal API Request Flow

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│ Frontend │                 │  Backend │                 │ Database │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │  1. GET /api/applications  │                            │
     │     Cookie: access_token   │                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │                            │  2. Extract JWT from cookie│
     │                            │  3. Validate signature     │
     │                            │  4. Check if revoked       │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  5. Token NOT revoked ✓    │
     │                            │                            │
     │                            │  6. Extract user info      │
     │                            │     - userId, email, roles │
     │                            │  7. Set SecurityContext    │
     │                            │                            │
     │                            │  8. Execute business logic │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  9. Query data             │
     │                            │                            │
     │  10. Return response       │                            │
     │<───────────────────────────┤                            │
```

### Token Refresh Flow (CURRENT: Forced OIDC Re-auth)

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│ Frontend │                 │  Backend │                 │   OIDC   │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │  1. Access token expires   │                            │
     │     (after 15 min)         │                            │
     │                            │                            │
     │  2. API call returns 401   │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  3. Frontend calls         │                            │
     │     POST /auth/refresh     │                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │  4. Return requiresReauth  │                            │
     │     (forced OIDC re-auth)  │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  5. Redirect to /auth/login│                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │  6. Redirect to OIDC       │  7. OIDC re-authentication │
     │<───────────────────────────┤───────────────────────────>│
     │                            │                            │
     │  8. OIDC checks session    │                            │
     │     - If active: Auto-issue new token (no password)     │
     │     - If expired: Prompt login                          │
     │<───────────────────────────┼────────────────────────────┤
     │                            │                            │
     │  9. New auth flow          │                            │
     │     (same as first login)  │                            │
```

### Token Refresh Flow (ALTERNATIVE: Silent Refresh - Disabled by Default)

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│ Frontend │                 │  Backend │                 │ Database │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │  1. Access token expires   │                            │
     │     (after 15 min)         │                            │
     │                            │                            │
     │  2. API call returns 401   │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  3. Frontend calls         │                            │
     │     POST /auth/refresh     │                            │
     │     Cookie: refresh_token  │                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │                            │  4. Hash refresh token     │
     │                            │  5. Query refresh_tokens   │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  6. Token found & valid ✓  │
     │                            │                            │
     │                            │  7. Get user roles         │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  8. Roles returned         │
     │                            │                            │
     │                            │  9. Generate NEW access    │
     │                            │     token (15 min)         │
     │                            │                            │
     │  10. Set new access_token  │                            │
     │      cookie                │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  11. Retry original API    │                            │
     │      call with new token   │                            │
```

### Logout Flow

```
┌──────────┐                 ┌──────────┐                 ┌──────────┐
│ Frontend │                 │  Backend │                 │ Database │
└────┬─────┘                 └────┬─────┘                 └────┬─────┘
     │                            │                            │
     │  1. User clicks "Logout"   │                            │
     ├───────────────────────────>│                            │
     │                            │                            │
     │                            │  2. Extract access token   │
     │                            │  3. Get JTI from token     │
     │                            │  4. Add to revoked_tokens  │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  5. Token blacklisted ✓    │
     │                            │                            │
     │                            │  6. Extract refresh token  │
     │                            │  7. Mark as revoked        │
     │                            ├───────────────────────────>│
     │                            │<───────────────────────────┤
     │                            │  8. Refresh token revoked ✓│
     │                            │                            │
     │  9. Clear cookies          │                            │
     │<───────────────────────────┤                            │
     │                            │                            │
     │  10. Redirect to login     │                            │
```

---

## 3. Automatic Session Extension (Proactive Token Refresh) {#proactive-token-refresh}

### Overview: Industry Standard Sliding Expiration

The application implements **proactive token refresh** - an industry-standard pattern that provides a user experience similar to traditional session-based authentication (like JSESSIONID) while maintaining the security benefits of JWT tokens.

**How It Works:**
- Frontend HTTP interceptor monitors token expiration time
- When token has **< 2 minutes** remaining AND user makes an API call
- Interceptor **automatically** calls `/auth/refresh` before the original request
- New token issued silently in the background
- Original API call proceeds with refreshed token
- User experiences **zero interruption**

### Implementation Details

**Frontend: AuthInterceptor (Angular)**

```typescript
// Location: frontend/src/app/interceptors/auth-interceptor.ts

// Key Constants
PROACTIVE_REFRESH_THRESHOLD_SECONDS = 120  // Refresh when < 2 min remaining
WARNING_THRESHOLD_SECONDS = 60              // Show dialog at < 1 min remaining

intercept(request: HttpRequest<any>, next: HttpHandler) {
  // Check session timer state
  const sessionState = this.sessionTimerService.getCurrentState();
  
  // Should we proactively refresh?
  const shouldProactivelyRefresh = 
    sessionState.tokenExpiresAt !== null && 
    sessionState.remainingSeconds > 0 &&
    sessionState.remainingSeconds <= 120 &&  // < 2 minutes
    !this.isRefreshing;

  if (shouldProactivelyRefresh) {
    // Refresh BEFORE making the API call
    return this.refreshToken().pipe(
      switchMap(() => next.handle(request))
    );
  }
  
  // Normal flow with 401 handling as fallback
  return next.handle(request);
}
```

**Backend: Token Refresh Endpoint**

```java
// Location: backend/src/main/java/x/y/z/backend/controller/AuthController.java

@PostMapping("/auth/refresh")
public ResponseEntity<Map<String, Object>> refreshToken() {
    // Silent refresh enabled by default (OPTION 2)
    // 1. Validate refresh token from cookie
    // 2. Generate new access token (15 min validity)
    // 3. Set new access_token cookie
    // 4. Return success response
    
    return ResponseEntity.ok(Map.of(
        "success", true,
        "expiresIn", 900  // 15 minutes
    ));
}
```

### Three-Layer Token Refresh Strategy

The application uses a **defense-in-depth** approach with three refresh triggers:

#### Layer 1: Proactive Refresh (Primary - Active Users)

**Trigger:** Token has < 2 minutes remaining AND user makes an API call

```
Timeline:
10:00 - User logs in (token expires 10:15)
10:13 - User navigates to Permits tab (13 min elapsed, 2 min remaining)
        ↓
        🔄 AUTOMATIC REFRESH TRIGGERED
        ↓
        New token issued (expires 10:28)
10:20 - User navigates to Applications tab (7 min elapsed, 8 min remaining)
        → No refresh needed yet
10:26 - User searches for records (13 min elapsed, 2 min remaining)
        ↓
        🔄 AUTOMATIC REFRESH TRIGGERED
        ↓
        New token issued (expires 10:41)
```

**User Experience:**
- ✅ Completely transparent - no interruption
- ✅ Session stays alive as long as user is active
- ✅ No dialogs or warnings shown
- ✅ Same experience as traditional session-based auth

**Security:**
- ✅ Token still expires if user inactive for 15 minutes
- ✅ Refresh token still expires after 7 days (forces re-authentication)
- ✅ Minimum privilege principle (short-lived access tokens)

#### Layer 2: Warning Dialog (Secondary - Inactive Users)

**Trigger:** Token has < 1 minute remaining (user was inactive for 14 minutes)

```
Timeline:
10:00 - User logs in
10:14 - User idle for 14 minutes (1 min remaining)
        ↓
        ⚠️ WARNING DIALOG SHOWN
        ↓
        "Session expiring in 60 seconds. Extend session?"
        [Extend] [Logout]
```

**User Experience:**
- User sees countdown timer in dialog
- Can manually extend session by clicking "Extend"
- If ignored, proceeds to Layer 3

**Security:**
- ✅ User acknowledges continued activity
- ✅ Prevents accidental session extension on unattended devices

#### Layer 3: Reactive Refresh (Fallback - Token Expired)

**Trigger:** API call returns 401 Unauthorized (token already expired)

```
Timeline:
10:00 - User logs in
10:15 - Token expires (user was completely inactive)
10:16 - User returns and clicks on a tab
        ↓
        API returns 401
        ↓
        Frontend catches error and tries to refresh
        ↓
        If refresh token valid: Silent refresh succeeds
        If refresh token expired: Redirect to login
```

**User Experience:**
- Brief delay while token refreshes
- If successful: User continues normally
- If failed: Redirected to login

**Security:**
- ✅ Last resort mechanism
- ✅ Handles edge cases (clock skew, network issues)

### Comparison with Traditional Sessions

| Feature | JSESSIONID (Session-based) | JWT + Proactive Refresh |
|---------|---------------------------|-------------------------|
| **Session Extension** | ✅ On every request | ✅ On requests when < 2 min remaining |
| **User Experience** | Seamless while active | ✅ Seamless while active |
| **Inactivity Timeout** | ✅ Configurable | ✅ 15 minutes (configurable) |
| **Absolute Timeout** | ✅ Configurable | ✅ 7 days (refresh token expiry) |
| **Scalability** | ❌ Server-side session storage | ✅ Stateless (no server storage) |
| **Load Balancer** | ⚠️ Sticky sessions required | ✅ Works across any server |
| **Database Load** | ❌ Session query on every request | ✅ Only blacklist check (optimized) |
| **Security** | ⚠️ Session fixation risks | ✅ Token rotation, revocation |

### Security Considerations

**✅ Safe Patterns (Implemented):**
1. **Short access token lifetime** (15 minutes)
   - Limits damage if token stolen
2. **Proactive refresh threshold** (2 minutes)
   - Balances UX and security
   - Prevents excessive refresh calls
3. **Refresh token expiration** (7 days)
   - Forces periodic re-authentication
4. **Token revocation on logout**
   - Blacklists both access and refresh tokens
5. **HttpOnly cookies**
   - Prevents JavaScript access to tokens
   - Mitigates XSS attacks

**❌ Anti-Patterns (Avoided):**
1. **Refreshing on every request** → Server overload
2. **No absolute timeout** → Indefinite sessions
3. **Long-lived access tokens** → Excessive exposure window
4. **Client-side token storage** → XSS vulnerability

### Industry Standards & References

**This pattern is endorsed by:**

1. **OAuth 2.0 RFC 6749** (Section 1.5)
   - "Refresh tokens are used to obtain new access tokens"
   - Recommends short-lived access tokens with refresh capability

2. **OWASP Authentication Cheat Sheet**
   - "Implement sliding session expiration"
   - "Refresh tokens before expiration for active users"

3. **Major Implementations:**
   - **Auth0**: Automatic token refresh before expiry
   - **Okta**: Silent token renewal for active sessions
   - **Azure AD**: Proactive refresh when < 5 min remaining
   - **Google OAuth**: Refresh token pattern with short access tokens

4. **NIST Digital Identity Guidelines (SP 800-63B)**
   - Section 7.2: "Use short-lived access tokens"
   - Section 4.3: "Implement inactivity timeouts"

### Configuration

**Frontend Constants:**
```typescript
// frontend/src/app/interceptors/auth-interceptor.ts
PROACTIVE_REFRESH_THRESHOLD_SECONDS = 120  // Refresh at 2 min remaining
```

**Backend Configuration:**
```properties
# backend/src/main/resources/application.properties
jwt.access-token-expiration-minutes=15  # Access token lifetime
jwt.refresh-token-expiration-days=7     # Refresh token lifetime (absolute timeout)
```

**Customization Options:**

| Threshold | Recommended | Trade-off |
|-----------|-------------|-----------|
| **30 seconds** | ⚠️ Too aggressive | More server load, less risk of expiry |
| **2 minutes** | ✅ Optimal | Balanced UX and performance |
| **5 minutes** | ⚠️ Conservative | Less server load, higher expiry risk |

**Choosing the Right Value:**
- **High-traffic apps**: 2-3 minutes (more overhead acceptable)
- **Low-traffic apps**: 3-5 minutes (reduce unnecessary refreshes)
- **Mobile apps**: 1-2 minutes (network latency considerations)

### Logging & Monitoring

The implementation includes debug logging:

```typescript
// Frontend console logs
[AuthInterceptor] Proactive token refresh triggered (119s remaining)
[AuthInterceptor] Proactive refresh successful - token extended
[AuthInterceptor] Reactive refresh successful (after 401)
```

**Metrics to Monitor:**
1. **Proactive refresh rate**: Should align with active user sessions
2. **Reactive refresh rate**: Should be low (fallback only)
3. **Failed refresh rate**: Indicates expired refresh tokens
4. **Session duration**: Average time between login and logout

### Testing the Behavior

**Local Development (2-minute access tokens):**

```powershell
# 1. Start backend and frontend
cd backend
.\run-local.ps1 Start

cd frontend
npm start

# 2. Login at http://localhost:4200
# 3. Open browser DevTools → Console
# 4. Wait 30 seconds, navigate to any tab
# 5. Watch for automatic refresh logs around 1:00 remaining
```

**Expected Console Output:**
```
10:00:00 - Login successful
10:01:00 - GET /api/permits/my (200 OK) - no refresh needed
10:01:30 - Session timer: 00:30 remaining
          [AuthInterceptor] Proactive token refresh triggered (29s remaining)
          POST /auth/refresh (200 OK)
          [AuthInterceptor] Proactive refresh successful
          [SessionTimer] Timer reset to 02:00
10:01:31 - GET /api/permits/my (200 OK) - with new token
```

---

## 4. Silent Refresh vs Forced OIDC Re-authentication {#silent-refresh-vs-forced-oidc}

### Current Implementation: Forced OIDC Re-auth (Enabled)

**How it Works:**
1. Access token expires (15 min)
2. Frontend calls `/auth/refresh`
3. Backend returns `requiresReauth: true`
4. Frontend redirects to `/auth/login`
5. OIDC provider re-authenticates user
6. New access token + refresh token issued

**Security Advantages:**
- ✅ Users periodically re-authenticate with OIDC provider
- ✅ OIDC session policies enforced (e.g., 2FA re-verification)
- ✅ Compliance with strict security requirements
- ✅ If OIDC provider revokes access, your app knows immediately

**User Experience:**
- ✅ Seamless if OIDC session active (no password re-entry)
- ⚠️ May prompt login if OIDC session expired
- ✅ Clear security boundary (periodic re-auth)

**Use Cases:**
- Banking/financial applications
- Healthcare systems (HIPAA compliance)
- Government/defense systems
- Any app with strict compliance requirements

**Code Location:**
```java
// In AuthController.java → refreshToken() method
// OPTION 1 is ENABLED by default (lines ~145-152)
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("requiresReauth", true);
errorResponse.put("loginUrl", "/auth/login");
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
```

---

### Alternative: Silent Refresh (Disabled by Default)

**How it Works:**
1. Access token expires (15 min)
2. Frontend calls `/auth/refresh`
3. Backend validates refresh token in database
4. New access token issued (same refresh token)
5. User continues seamlessly (no redirect)

**Security Trade-offs:**
- ❌ Users never return to OIDC provider (sessions can last 7 days)
- ❌ If OIDC provider revokes access, your app doesn't know
- ✅ Simpler token management
- ✅ Works offline (if refresh token valid)

**User Experience:**
- ✅ Completely seamless (no interruption)
- ✅ No periodic login prompts
- ⚠️ False sense of security (may not be actually authorized)

**Use Cases:**
- Low-risk internal tools
- Mobile apps (offline capability)
- Developer tools
- Prototypes/demos

**How to Enable:**
```java
// In AuthController.java → refreshToken() method
// Step 1: Comment out OPTION 1 (lines ~145-152)
/*
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("requiresReauth", true);
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
*/

// Step 2: Uncomment OPTION 2 (lines ~154-210)
// The silent refresh code is already there, just commented out
```

---

### Decision Matrix: Which Should You Use?

| Criteria | Forced OIDC | Silent Refresh |
|----------|-------------|----------------|
| **Security Level** | ⭐⭐⭐⭐⭐ High | ⭐⭐⭐ Medium |
| **Compliance** | ✅ HIPAA, SOC2, PCI-DSS | ⚠️ May not meet strict requirements |
| **User Experience** | ⭐⭐⭐⭐ Good (if OIDC session active) | ⭐⭐⭐⭐⭐ Excellent |
| **OIDC Integration** | ✅ Full integration | ⚠️ Initial only |
| **Offline Support** | ❌ Requires OIDC connectivity | ✅ Works for 7 days |
| **Token Lifespan** | 15 min (periodic re-auth) | Up to 7 days |
| **Database Load** | Lower (only on re-auth) | Higher (every 15 min) |
| **Implementation** | Enabled by default | Commented out (easy to enable) |

**Recommendation:** 
- **Production apps with compliance requirements**: Use Forced OIDC (current default)
- **Internal tools or low-risk apps**: Consider Silent Refresh
- **Hybrid approach**: Use Silent Refresh during business hours, Forced OIDC after hours

---

## 4. Future Performance Improvement with Redis Cache {#redis-cache-optimization}

### Current Performance Bottleneck

**Every authenticated request does this:**
```java
// In JwtTokenService.validateAccessToken()
String jti = jwtTokenUtil.getJtiFromToken(jwt);
return !revokedTokenHandler.isRevoked(jti); // ← DATABASE QUERY
```

**Impact:**
- 100 requests/second = 100 database queries/second
- 1000 requests/second = 1000 database queries/second
- Database becomes bottleneck at scale

### Solution: Redis Cache for Revoked Tokens

**Why Redis?**
- ⚡ In-memory = 10-100x faster than database
- 🔄 Built-in TTL (time-to-live) expiration
- 🌐 Distributed cache (works with multiple backend instances)
- 📊 Minimal overhead (<1ms latency)

### Implementation Plan

#### Step 1: Add Redis Dependencies

```xml
<!-- In pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.lettuce.core</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>
```

#### Step 2: Configure Redis

```properties
# In application.properties
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}
spring.redis.password=${REDIS_PASSWORD:}
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
```

#### Step 3: Update JwtTokenService

```java
@Service
public class JwtTokenService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final RevokedTokenHandler revokedTokenHandler;
    
    @Transactional(readOnly = true)
    public boolean validateAccessToken(String accessToken) {
        try {
            jwtTokenUtil.validateToken(accessToken);
            String jti = jwtTokenUtil.getJtiFromToken(accessToken);
            
            // ✨ Check Redis cache first (fast)
            String cacheKey = "revoked:" + jti;
            Boolean cachedValue = redisTemplate.hasKey(cacheKey);
            
            if (Boolean.TRUE.equals(cachedValue)) {
                return false; // Token is revoked (from cache)
            }
            
            // 🔍 Fallback to database (only if not in cache)
            boolean isRevoked = revokedTokenHandler.isRevoked(jti);
            
            // 💾 Update cache if token is revoked
            if (isRevoked) {
                long ttl = getTokenRemainingTtl(accessToken); // Calculate remaining lifetime
                redisTemplate.opsForValue().set(cacheKey, "true", ttl, TimeUnit.SECONDS);
            }
            
            return !isRevoked;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public void revokeAccessToken(String accessToken, String reason) {
        String jti = jwtTokenUtil.getJtiFromToken(accessToken);
        UUID userId = jwtTokenUtil.getUserIdFromToken(accessToken);
        LocalDateTime expiresAt = /* extract from token */;
        
        // Add to database (persistent)
        RevokedToken revokedToken = new RevokedToken(jti, userId, expiresAt, reason);
        revokedTokenHandler.insert(revokedToken);
        
        // ✨ Add to Redis cache (fast lookup)
        String cacheKey = "revoked:" + jti;
        long ttl = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        redisTemplate.opsForValue().set(cacheKey, "true", ttl, TimeUnit.SECONDS);
    }
}
```

#### Step 4: Deploy Redis

**Docker Compose (Local Development):**
```yaml
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD:-changeme}
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  redis_data:
```

**Azure (Production):**
```bash
# Azure Cache for Redis
az redis create \
  --resource-group rg-raptor-dev \
  --name redis-raptor-dev \
  --location eastus2 \
  --sku Basic \
  --vm-size c0
```

### Performance Comparison

| Metric | Database Only | Redis Cache |
|--------|---------------|-------------|
| **Latency (avg)** | 10-50ms | <1ms |
| **Throughput** | ~500 req/sec | ~10,000 req/sec |
| **Database Load** | High (every request) | Low (cache misses only) |
| **Cache Hit Rate** | N/A | ~95-99% |
| **Cost** | Free (existing DB) | ~$15-50/month (Azure) |

### Cache Invalidation Strategy

**Automatic Expiration:**
- Redis TTL = Access token expiration time
- Expired tokens auto-removed from cache
- No manual cleanup needed

**Manual Cleanup (Optional):**
```java
@Scheduled(cron = "0 0 * * * *") // Every hour
public void cleanupExpiredTokens() {
    revokedTokenHandler.deleteExpired(LocalDateTime.now());
    // Redis auto-expires, no cleanup needed
}
```

---

## 5. Admin Features: Active User Monitoring {#admin-active-users}

### Current Implementation

**Endpoint:** `GET /api/admin/users/active`

**What it Returns:**
- Users with `is_active = true` (not deactivated by admin)
- User details: ID, email, name, roles, last login time
- **Note:** This shows accounts that are active, not real-time sessions

**Example Response:**
```json
{
  "success": true,
  "total": 5,
  "users": [
    {
      "id": "uuid",
      "email": "john.doe@example.com",
      "fullName": "John Doe",
      "roles": ["USER", "ADMIN"],
      "isActive": true,
      "lastLoginAt": "2025-11-05T10:30:00",
      "createdAt": "2025-11-01T09:00:00"
    }
  ],
  "note": "Shows users with active accounts. For real-time session tracking, consider implementing session management or Redis cache."
}
```

### Limitation: Not Real-Time Sessions

**Current approach tracks:**
- ✅ Users who have logged in before
- ✅ Users who are not deactivated
- ✅ Last login timestamp
- ❌ **NOT** users currently online

**Why?**
- Access tokens are stateless (not stored in DB)
- Only revoked tokens are in DB
- No session table tracking "who's online now"

### Future Enhancement: Real-Time Active Sessions

#### Option 1: Session Management Table

**Create new table:**
```sql
CREATE TABLE user_sessions (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    access_token_jti NVARCHAR(255) NOT NULL,
    ip_address NVARCHAR(50),
    user_agent NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    last_activity_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    expires_at DATETIME2 NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX IX_user_sessions_user_id (user_id),
    INDEX IX_user_sessions_expires_at (expires_at)
);
```

**Update AuthController.handleOidcCallback():**
```java
// After generating tokens
SessionInfo session = new SessionInfo(
    user.getId(),
    jti, // JWT ID
    request.getRemoteAddr(),
    request.getHeader("User-Agent"),
    LocalDateTime.now().plusMinutes(15)
);
sessionHandler.insert(session);
```

**New Admin Endpoint:**
```java
@GetMapping("/users/sessions/active")
public ResponseEntity<?> getActiveSessions() {
    // Query sessions where expires_at > now AND not revoked
    List<SessionInfo> activeSessions = sessionHandler.findActiveSessions();
    
    // Group by user
    Map<UUID, List<SessionInfo>> sessionsByUser = activeSessions.stream()
        .collect(Collectors.groupingBy(SessionInfo::getUserId));
    
    return ResponseEntity.ok(sessionsByUser);
}
```

#### Option 2: Redis Session Tracking (Recommended)

**Store session in Redis on token generation:**
```java
public TokenPair generateTokens(UUID userId) {
    String accessToken = jwtTokenUtil.generateAccessToken(userId, email, roles);
    String jti = jwtTokenUtil.getJtiFromToken(accessToken);
    
    // Store session in Redis
    String sessionKey = "session:" + userId + ":" + jti;
    Map<String, String> sessionData = Map.of(
        "userId", userId.toString(),
        "email", email,
        "jti", jti,
        "createdAt", Instant.now().toString()
    );
    redisTemplate.opsForHash().putAll(sessionKey, sessionData);
    redisTemplate.expire(sessionKey, 15, TimeUnit.MINUTES); // Auto-expire
    
    return new TokenPair(accessToken, refreshToken);
}
```

**Track active users in Redis Set:**
```java
// On login
redisTemplate.opsForSet().add("active_users", userId.toString());
redisTemplate.expire("active_users", 15, TimeUnit.MINUTES);

// On token validation (update activity)
redisTemplate.opsForSet().add("active_users", userId.toString());

// Get active users
Set<String> activeUserIds = redisTemplate.opsForSet().members("active_users");
```

**Admin endpoint:**
```java
@GetMapping("/users/online")
public ResponseEntity<?> getOnlineUsers() {
    Set<String> activeUserIds = redisTemplate.opsForSet().members("active_users");
    
    List<User> onlineUsers = activeUserIds.stream()
        .map(id -> userService.findById(UUID.fromString(id)))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    
    return ResponseEntity.ok(Map.of(
        "total", onlineUsers.size(),
        "users", onlineUsers
    ));
}
```

### Comparison: Session Tracking Approaches

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| **Database Table** | Persistent, queryable history | Slower, more DB load | Audit logs, compliance |
| **Redis Only** | Fast, real-time, auto-expire | Lost on Redis restart | Live dashboards, online users |
| **Hybrid (DB + Redis)** | Best of both worlds | More complex | Production apps |

### Security Considerations

**Admin Authorization:**
- All `/api/admin/*` endpoints require `ROLE_ADMIN`
- Implemented with `@PreAuthorize("hasRole('ADMIN')")`
- Returns 403 Forbidden for non-admin users

**Privacy Concerns:**
- Don't expose sensitive user data (passwords, tokens)
- Consider data retention policies (GDPR compliance)
- Log admin actions (who viewed/modified user data)

---

## Summary

### Current Architecture (Production-Ready)

✅ **Two-token strategy**: Access (15 min) + Refresh (7 days)  
✅ **Forced OIDC re-auth**: Enabled by default for security  
✅ **Revocation checks**: Every request validates against database  
✅ **Admin endpoints**: User management and role assignment  
⚠️ **Performance**: DB query on every request (optimize with Redis)  
⚠️ **Active users**: Shows accounts, not real-time sessions (enhance with Redis)

### Next Steps for Enhancement

1. **Add Redis Cache** (High Priority - Performance)
   - Cache revoked tokens
   - Reduce database load by 95%+
   - ~1 day to implement

2. **Real-Time Session Tracking** (Medium Priority - UX)
   - Track active sessions in Redis
   - Admin dashboard showing "users online now"
   - ~2 days to implement

3. **Silent Refresh Option** (Low Priority - Flexibility)
   - Already implemented (commented out)
   - Enable if needed for specific use cases
   - ~1 hour to enable and test

### Configuration Quick Reference

```java
// Enable forced OIDC re-auth (DEFAULT):
// AuthController.refreshToken() → OPTION 1 is uncommented

// Enable silent refresh:
// AuthController.refreshToken() → Comment OPTION 1, uncomment OPTION 2

// Token expiry settings:
jwt.access-token-expiration-minutes=15      // Access token: 15 min
jwt.refresh-token-expiration-days=7         // Refresh token: 7 days

// Admin features:
// GET /api/admin/users/active                 // All active accounts
// GET /api/admin/users/sessions/active        // (Future) Real-time sessions
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-05  
**Maintained By:** RAP Development Team
