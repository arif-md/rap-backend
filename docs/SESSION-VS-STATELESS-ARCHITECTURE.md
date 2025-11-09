# Session vs Stateless Architecture in OAuth2/OIDC

## Overview

This document explains our hybrid authentication architecture that combines **session-based OAuth2 login** with **stateless JWT-based API access**.

---

## Architecture Design

### 1. **OAuth2 Login Flow (Session-Based - Temporary)**

During the OIDC/OAuth2 authorization code flow, sessions are **required** but only temporarily:

```
┌─────────┐         ┌─────────┐         ┌──────────┐
│ Browser │         │ Backend │         │ Keycloak │
└────┬────┘         └────┬────┘         └────┬─────┘
     │                   │                    │
     │  1. /auth/login   │                    │
     ├──────────────────>│                    │
     │                   │                    │
     │  2. Redirect to   │                    │
     │  /oauth2/auth...  │                    │
     │<──────────────────┤                    │
     │   (Session Created: stores CSRF + state)
     │                   │                    │
     │  3. Redirect to Keycloak              │
     ├───────────────────────────────────────>│
     │                   │                    │
     │  4. User login    │                    │
     │                   │                    │
     │  5. Callback with auth code           │
     │<───────────────────────────────────────┤
     │                   │                    │
     │  6. /auth/callback?code=...&state=...  │
     ├──────────────────>│                    │
     │   (Session Read: validates state)      │
     │                   │                    │
     │                   │  7. Exchange code  │
     │                   │    for tokens      │
     │                   ├───────────────────>│
     │                   │                    │
     │                   │  8. access_token   │
     │                   │     id_token       │
     │                   │<───────────────────┤
     │                   │                    │
     │  9. Set JWT cookies (HttpOnly)         │
     │<──────────────────┤                    │
     │   (Session Destroyed or Expires)       │
     │                   │                    │
```

**Why sessions are needed during OAuth2 flow:**
- **CSRF Protection**: Spring Security stores CSRF tokens in the session
- **State Validation**: OAuth2 state parameter must be correlated with the original request
- **Authorization Request Details**: Client ID, redirect URI, scope stored temporarily

**Session Lifetime:** Only during the OAuth2 handshake (~30-60 seconds max)

---

### 2. **API Access (Stateless - JWT Tokens)**

After successful OAuth2 login, all API requests use JWT tokens:

```
┌─────────┐         ┌─────────┐         ┌──────────┐
│ Browser │         │ Backend │         │ Database │
└────┬────┘         └────┬────┘         └────┬─────┘
     │                   │                    │
     │  GET /api/applications                 │
     │  Cookie: access_token=<jwt>            │
     ├──────────────────>│                    │
     │                   │                    │
     │                   │ (No session lookup)│
     │                   │ (Validate JWT sig) │
     │                   │ (Check expiry)     │
     │                   │                    │
     │                   │  Query DB          │
     │                   ├───────────────────>│
     │                   │                    │
     │                   │  Results           │
     │                   │<───────────────────┤
     │                   │                    │
     │  Response         │                    │
     │<──────────────────┤                    │
     │                   │                    │
```

**Stateless Characteristics:**
- No session lookup required
- JWT contains all user identity info (subject, roles, claims)
- Each request is self-contained
- Backend can scale horizontally without session replication

---

## Configuration Explanation

### Spring Security Session Policy

```java
.sessionManagement(session -> 
    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
```

**`SessionCreationPolicy.IF_REQUIRED`** means:
- ✅ Sessions created **only when Spring Security needs them** (OAuth2 login)
- ✅ JWT-authenticated requests **do NOT create sessions** (stateless)
- ✅ Existing sessions reused if present
- ❌ Sessions NOT created for every request

### Endpoint-Specific Behavior

| Endpoint Pattern | Authentication Method | Session Required? | Notes |
|-----------------|----------------------|-------------------|-------|
| `/auth/login` | None (public) | No | Returns redirect URL |
| `/oauth2/**` | OAuth2 flow | **Yes (temporary)** | Spring Security manages |
| `/auth/callback` | OAuth2 callback | **Yes (validates state)** | Reads session, creates JWT |
| `/api/**` | JWT token | **No** | Stateless - JWT in cookie/header |
| `/auth/refresh` | Refresh token | **No** | Stateless token exchange |

---

## Containerized/Multi-Instance Deployment

### Problem: Default In-Memory Sessions Don't Work

❌ **Won't work with multiple backend containers:**
```
Load Balancer
     ├─> Backend Instance 1 (has session for user A)
     ├─> Backend Instance 2 (no session for user A)
     └─> Backend Instance 3 (no session for user A)
```

If the OAuth2 callback lands on a different instance than the one that initiated the login, the session won't be found → authentication fails.

### Solution: Sticky Sessions OR Redis Session Store

#### **Option 1: Sticky Sessions (Simplest)**

Configure your load balancer (Azure Container Apps, Kubernetes Ingress, etc.) to route all requests from the same client to the same backend instance during the OAuth2 flow:

**Azure Container Apps:**
```yaml
sessionAffinity:
  affinity: sticky
```

**Nginx Ingress:**
```yaml
nginx.ingress.kubernetes.io/affinity: "cookie"
nginx.ingress.kubernetes.io/session-cookie-name: "route"
nginx.ingress.kubernetes.io/session-cookie-expires: "172800"
```

**Pros:**
- No code changes needed
- Simple configuration
- Works for temporary session needs

**Cons:**
- Uneven load distribution during login bursts
- Sessions lost if container restarts during OAuth2 flow (rare, short window)

---

#### **Option 2: Shared Session Store (Redis) - Production Recommended**

Use Spring Session with Redis for distributed session storage:

**Dependencies (pom.xml):**
```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Configuration (application.properties):**
```properties
# Redis session store
spring.session.store-type=redis
spring.session.timeout=300s
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}

# Only store sessions for specific URLs (OAuth2 flow)
spring.session.redis.namespace=spring:session:oauth2
```

**Pros:**
- Works with any number of backend instances
- Sessions persist across container restarts
- Proper high-availability setup

**Cons:**
- Requires Redis deployment
- Additional infrastructure complexity
- Slight latency overhead (negligible for login flow)

---

## Current Implementation Status

### ✅ What We Have Now

1. **Hybrid Architecture**: OAuth2 uses sessions, APIs use JWT (stateless)
2. **Local Development**: Single backend container, in-memory sessions work fine
3. **JWT-based API Access**: All `/api/**` endpoints are stateless

### ⚠️ What Needs to Be Added for Production

**For Azure deployment with multiple backend containers:**

**Recommended: Add Redis session store**

1. Deploy Azure Cache for Redis
2. Add Spring Session dependencies
3. Configure Redis connection in `main.bicep`:

```bicep
// Add Redis connection to backend container app
env: [
  {
    name: 'REDIS_HOST'
    value: redisCache.properties.hostName
  }
  {
    name: 'REDIS_PORT'
    value: '6380'  // Azure Redis uses SSL port
  }
  {
    name: 'REDIS_PASSWORD'
    secretRef: 'redis-password'
  }
]
```

**Alternative: Use sticky sessions**

Azure Container Apps supports session affinity natively - enable it in the infrastructure.

---

## Testing Multi-Instance Scenarios

### Simulate Load Balancing Locally

Run multiple backend instances with Docker Compose:

```yaml
services:
  backend-1:
    build: .
    ports:
      - "8081:8080"
    environment:
      - SPRING_SESSION_STORE_TYPE=redis
  
  backend-2:
    build: .
    ports:
      - "8082:8080"
    environment:
      - SPRING_SESSION_STORE_TYPE=redis
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

Test OAuth2 flow:
1. Start login on backend-1 (localhost:8081)
2. Callback lands on backend-2 (localhost:8082)
3. Should succeed if Redis session store configured

---

## FAQ

### Q: Why not make OAuth2 login completely stateless?

**A:** The OAuth2 specification **requires** state validation for security (CSRF protection). Spring Security implements this using sessions. While theoretically possible to use stateless alternatives (JWT-based state), it's not supported out-of-the-box and increases complexity.

### Q: Will every API request check the database?

**A:** No. JWT tokens are self-contained:
- User identity, roles, expiration are in the JWT payload
- Backend validates JWT signature (cryptographic operation, no DB lookup)
- Only token refresh or user permission changes require DB access

### Q: How long do sessions last?

**A:** Very short:
- Created when `/oauth2/authorization/oidc-provider` is called
- Destroyed after `/auth/callback` completes (~30-60 seconds max)
- Default Spring Session timeout: 30 minutes (but we convert to JWT immediately)

### Q: What happens if a container crashes during OAuth2 login?

**Without Redis:**
- User sees error, must retry login
- Session lost (very small time window, rare occurrence)

**With Redis:**
- Session persists in Redis
- User redirected to any healthy backend instance
- Login completes successfully

---

## Recommended Next Steps

### For Local Development (Current)
✅ No changes needed - single container works fine with in-memory sessions

### For Azure Production Deployment

**Option A: Quick Deploy (Sticky Sessions)**
1. Enable session affinity in Azure Container Apps
2. Deploy as-is
3. Monitor for session-related errors

**Option B: Production-Ready (Redis)**
1. Add Spring Session dependencies to `pom.xml`
2. Deploy Azure Cache for Redis via Bicep
3. Configure Redis connection in backend container environment
4. Test multi-instance locally with Redis

**Recommendation:** Start with Option A (sticky sessions) for MVP, migrate to Option B (Redis) for production scale.

---

## Summary

| Aspect | OAuth2 Login Flow | API Access |
|--------|------------------|------------|
| **Session State** | Temporary (30-60s) | None |
| **Storage** | In-memory or Redis | JWT in cookie/header |
| **Scalability** | Needs sticky sessions OR Redis | Fully stateless, scales infinitely |
| **Multi-instance** | Requires coordination | Works out-of-the-box |
| **RESTful?** | No (temporary session) | ✅ Yes (100% stateless) |

**Bottom Line:** 
- ✅ REST APIs remain stateless (99.9% of requests)
- ⚠️ OAuth2 login uses sessions temporarily (0.1% of requests, ~60s duration)
- ✅ Production-ready with sticky sessions OR Redis session store
