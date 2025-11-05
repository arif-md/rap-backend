# OIDC Authentication Implementation Guide

## Architecture Overview

### Authentication Flow
```
┌─────────┐      ┌─────────┐      ┌──────────────┐      ┌──────────┐
│Frontend │      │Backend  │      │OIDC Provider │      │Database  │
└────┬────┘      └────┬────┘      └──────┬───────┘      └────┬─────┘
     │                │                   │                   │
     │ 1. /auth/login │                   │                   │
     ├───────────────>│                   │                   │
     │                │ 2. Redirect to    │                   │
     │                │    OIDC login     │                   │
     │                ├──────────────────>│                   │
     │                │                   │                   │
     │ 3. User authenticates              │                   │
     │                │                   │                   │
     │                │ 4. Auth code      │                   │
     │                │<──────────────────┤                   │
     │                │                   │                   │
     │                │ 5. Exchange code  │                   │
     │                │    + client secret│                   │
     │                ├──────────────────>│                   │
     │                │                   │                   │
     │                │ 6. OIDC tokens    │                   │
     │                │<──────────────────┤                   │
     │                │                   │                   │
     │                │ 7. Check user roles                   │
     │                ├───────────────────────────────────────>│
     │                │                   │                   │
     │                │ 8. Roles data     │                   │
     │                │<───────────────────────────────────────┤
     │                │                   │                   │
     │                │ 9. Generate JWT   │                   │
     │                │    (signed with   │                   │
     │                │     JWT secret)   │                   │
     │                │                   │                   │
     │ 10. Redirect   │                   │                   │
     │     with JWT   │                   │                   │
     │<───────────────┤                   │                   │
     │                │                   │                   │
     │ 11. Access     │                   │                   │
     │     dashboard  │                   │                   │
     │                │                   │                   │
     │ 12. API call   │                   │                   │
     │     + JWT      │                   │                   │
     ├───────────────>│                   │                   │
     │                │ 13. Verify JWT    │                   │
     │                │     signature     │                   │
     │                │                   │                   │
     │                │ 14. Check authz   │                   │
     │                │<──────────────────────────────────────┤
     │                │                   │                   │
     │ 15. Response   │                   │                   │
     │<───────────────┤                   │                   │
```

### Token Strategy

**Two Types of Tokens:**

1. **OIDC Tokens** (from external provider)
   - ID Token: User identity claims
   - Access Token: For calling OIDC provider APIs (optional)
   - Refresh Token: Get new OIDC tokens (optional)
   - **Used for**: Authentication only
   - **Stored**: Backend session only (never sent to frontend)

2. **Application JWT** (issued by your backend)
   - Signed with your JWT secret
   - Contains user info + roles from your database
   - **Used for**: Authorization and session management
   - **Stored**: Frontend (httpOnly cookie OR sessionStorage)
   - **Lifespan**: 15 minutes (configurable)

### Security Secrets

| Secret | Purpose | Storage | Access |
|--------|---------|---------|--------|
| **OIDC Client Secret** | Authenticate backend with OIDC provider | Azure Key Vault → Backend env var | Backend only |
| **JWT Signing Secret** | Sign/verify application JWTs | Azure Key Vault → Backend env var | Backend only |
| **JWT Encryption Key** (optional) | Encrypt JWT payload | Azure Key Vault → Backend env var | Backend only |

### Configuration Parameters

| Parameter | Example | Storage | Description |
|-----------|---------|---------|-------------|
| `OIDC_ISSUER_URI` | `https://accounts.google.com` | Environment variable | OIDC provider base URL |
| `OIDC_CLIENT_ID` | `raptor-app-client-id` | Environment variable | Public client identifier |
| `OIDC_CLIENT_SECRET` | `*********` | Key Vault reference | Secret for token exchange |
| `JWT_SECRET` | `*********` | Key Vault reference | Secret for JWT signing |
| `JWT_EXPIRATION_MINUTES` | `15` | Environment variable | JWT lifespan |
| `SESSION_TIMEOUT_MINUTES` | `15` | Environment variable | Frontend idle timeout |
| `OIDC_REDIRECT_URI` | `https://backend.com/auth/callback` | Environment variable | Callback URL after OIDC auth |

## Step 1: Database Schema - User & Roles

### Tables Required

```sql
-- Users table (stores OIDC authenticated users)
CREATE TABLE users (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    email NVARCHAR(255) NOT NULL UNIQUE,
    full_name NVARCHAR(255),
    oidc_subject NVARCHAR(255) NOT NULL UNIQUE, -- 'sub' claim from OIDC provider
    is_active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    last_login_at DATETIME2
);

-- Roles table (application roles for authorization)
CREATE TABLE roles (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    role_name NVARCHAR(50) NOT NULL UNIQUE, -- e.g., 'USER', 'ADMIN', 'MANAGER'
    description NVARCHAR(255),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- User-Role mapping (many-to-many)
CREATE TABLE user_roles (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    role_id UNIQUEIDENTIFIER NOT NULL,
    granted_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    granted_by NVARCHAR(255), -- Who granted this role (for audit)
    CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT UQ_user_role UNIQUE (user_id, role_id)
);

-- Indexes for performance
CREATE INDEX IX_users_email ON users(email);
CREATE INDEX IX_users_oidc_subject ON users(oidc_subject);
CREATE INDEX IX_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IX_user_roles_role_id ON user_roles(role_id);

-- Seed default roles
INSERT INTO roles (role_name, description) VALUES
('USER', 'Basic authenticated user'),
('ADMIN', 'System administrator'),
('MANAGER', 'Application manager');
```

### JWT Revocation (Optional - Advanced)

For immediate JWT revocation (e.g., logout, security breach):

```sql
-- Revoked JWT tracking
CREATE TABLE revoked_tokens (
    id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    jti NVARCHAR(255) NOT NULL UNIQUE, -- JWT ID
    user_id UNIQUEIDENTIFIER NOT NULL,
    revoked_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    expires_at DATETIME2 NOT NULL, -- When JWT would have expired naturally
    reason NVARCHAR(255), -- 'LOGOUT', 'SECURITY_BREACH', 'PASSWORD_CHANGE'
    CONSTRAINT FK_revoked_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IX_revoked_tokens_jti ON revoked_tokens(jti);
CREATE INDEX IX_revoked_tokens_expires_at ON revoked_tokens(expires_at);

-- Cleanup job: Delete expired revoked tokens (no longer needed)
-- DELETE FROM revoked_tokens WHERE expires_at < GETUTCDATE();
```

## Step 2: Backend Configuration (application.properties)

```properties
# ===================================================================
# OIDC OAuth2 Client Configuration
# ===================================================================
spring.security.oauth2.client.registration.oidc-provider.client-id=${OIDC_CLIENT_ID}
spring.security.oauth2.client.registration.oidc-provider.client-secret=${OIDC_CLIENT_SECRET}
spring.security.oauth2.client.registration.oidc-provider.scope=openid,profile,email
spring.security.oauth2.client.registration.oidc-provider.redirect-uri=${OIDC_REDIRECT_URI:http://localhost:8080/auth/callback}
spring.security.oauth2.client.registration.oidc-provider.client-name=External OIDC Provider

spring.security.oauth2.client.provider.oidc-provider.issuer-uri=${OIDC_ISSUER_URI}
# OR manually configure endpoints if issuer-uri is not supported:
# spring.security.oauth2.client.provider.oidc-provider.authorization-uri=${OIDC_ISSUER_URI}/authorize
# spring.security.oauth2.client.provider.oidc-provider.token-uri=${OIDC_ISSUER_URI}/token
# spring.security.oauth2.client.provider.oidc-provider.user-info-uri=${OIDC_ISSUER_URI}/userinfo
# spring.security.oauth2.client.provider.oidc-provider.jwk-set-uri=${OIDC_ISSUER_URI}/keys

# ===================================================================
# JWT Configuration (Your Application Tokens)
# ===================================================================
jwt.secret=${JWT_SECRET}
jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:15}
jwt.issuer=${JWT_ISSUER:https://raptor-backend.azurecontainerapps.io}
jwt.audience=${JWT_AUDIENCE:raptor-frontend}

# ===================================================================
# Frontend URL (for redirects after auth)
# ===================================================================
app.frontend.url=${FRONTEND_URL:http://localhost:4200}
app.frontend.dashboard-url=${app.frontend.url}/dashboard

# ===================================================================
# CORS Configuration
# ===================================================================
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200,https://your-frontend.azurecontainerapps.io}
app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
app.cors.allowed-headers=*
app.cors.allow-credentials=true
```

## Step 3: JWT Token Payload Design

### Claims to Include

```json
{
  "sub": "auth0|507f1f77bcf86cd799439011",  // OIDC subject (unique user ID)
  "iss": "https://raptor-backend.com",       // Issuer (your backend)
  "aud": "raptor-frontend",                  // Audience (your frontend)
  "exp": 1730755900,                         // Expiration timestamp
  "iat": 1730755000,                         // Issued at timestamp
  "jti": "550e8400-e29b-41d4-a716-446655440000", // JWT ID (for revocation)
  
  // Custom claims (readable by frontend)
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe",
  "roles": ["USER", "ADMIN"],
  "permissions": ["read:applications", "write:applications"]  // Optional fine-grained
}
```

### Security Considerations

✅ **Do:**
- Keep JWT small (< 8KB for cookie storage)
- Include only necessary claims
- Use standard claims (sub, iss, aud, exp, iat)
- Include roles for frontend UI decisions
- Use short expiration (15 min)
- Sign with HS512 or RS256

❌ **Don't:**
- Include sensitive data (passwords, SSNs, credit cards)
- Include large data (profile pictures, full documents)
- Store in localStorage if sensitive (prefer httpOnly cookie)
- Make expiration > 1 hour

## Next Steps to Review & Approve

**Phase 1 - Database & Config** (Current):
1. ✅ Review database schema above
2. ✅ Review configuration parameters
3. ✅ Review JWT payload design
4. ✅ Approve to proceed with Flyway migration creation

**Phase 2 - Backend Security** (Next):
5. Security configuration (SecurityConfig.java)
6. JWT token service (generation, validation, refresh)
7. User service & repository (MyBatis mappers)

**Phase 3 - Backend Controllers** (After Phase 2):
8. Authentication controller (/auth/* endpoints)
9. JWT filter for request validation

**Phase 4 - Frontend Auth** (After Phase 3):
10. Auth service & interceptors
11. Login component & dashboard
12. Session timeout logic

**Phase 5 - Infrastructure** (After Phase 4):
13. Key Vault integration
14. Environment variables in Bicep
15. Testing

---

## Questions for You

Before proceeding with Flyway migration:

1. **OIDC Provider**: Which provider will you use?
   - Azure AD (Entra ID)
   - Google
   - Auth0
   - Okta
   - Keycloak
   - Other?

2. **JWT Storage**: Where should frontend store JWT?
   - **Option A**: HttpOnly cookie (more secure, prevents XSS)
   - **Option B**: SessionStorage (easier to read in JS, vulnerable to XSS)
   - **Option C**: Hybrid (Cookie for auth, localStorage for claims)

3. **Refresh Token**: Do you want refresh token support?
   - **Yes**: Long-lived refresh token to get new JWT without re-login
   - **No**: User must re-authenticate after 15 min

4. **Role Assignment**: How will users get roles?
   - Manual admin assignment
   - Auto-assign default "USER" role on first login
   - Both?

Please review the schema and answer these questions, then I'll proceed with implementation!
