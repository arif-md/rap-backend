# OAuth2 Hostname Mismatch - Solution Summary

## Problem

When implementing OAuth2 authentication with Keycloak in a Docker environment, we encountered a critical hostname mismatch issue:

1. **Browser Flow**: User's browser accesses Keycloak at `http://localhost:9090`
2. **Backend Container**: Must access Keycloak at `http://host.docker.internal:9090` (Docker's host gateway)
3. **Keycloak Validation**: Rejects UserInfo endpoint requests when the hostname doesn't match the token's issuer

**Error**: `[invalid_user_info_response] 401 Unauthorized: [no body]`

## Root Cause

Keycloak validates that:
- Access tokens are issued with `iss=http://localhost:9090/realms/raptor` (from browser flow)
- UserInfo endpoint calls must use the EXACT SAME hostname as the token issuer
- Backend calling `host.docker.internal:9090/userinfo` doesn't match `localhost:9090` → 401 Unauthorized

## Solution: Custom OidcUserService

**Implementation**: `CustomOidcUserService.java`

Instead of calling Keycloak's UserInfo endpoint, we extract all user information from the **ID Token claims** which are already validated via JWK Set (cryptographic signature verification).

### How It Works

1. **OAuth2 Flow Unchanged**:
   - Browser → `localhost:9090` → User authenticates → Callback with authorization code
   - Backend → `host.docker.internal:9090/token` → Exchange code for tokens (access_token + id_token)

2. **Custom User Loading**:
   ```java
   @Override
   public OidcUser loadUser(OidcUserRequest userRequest) {
       // Get ID Token (already validated by Spring Security via JWK Set)
       var idToken = userRequest.getIdToken();
       Map<String, Object> claims = idToken.getClaims();
       
       // Extract user info from claims (no UserInfo endpoint call)
       Set<GrantedAuthority> authorities = extractAuthorities(claims);
       
       // Create OidcUser from ID Token only
       return new DefaultOidcUser(authorities, idToken, "preferred_username");
   }
   ```

3. **No UserInfo Call**:
   - Skip `GET /userinfo` endpoint entirely
   - Extract all user data from ID token claims:
     - `sub` (subject/user ID)
     - `preferred_username`
     - `email`
     - `realm_access.roles` (Keycloak realm roles)
     - `resource_access.{client}.roles` (client-specific roles)

### Benefits

✅ **Works Locally**: Handles mixed hostnames (localhost for browser, host.docker.internal for backend)
✅ **Works in Azure**: When both browser and backend use same public URL, still works
✅ **Secure**: ID Token is cryptographically validated via JWK Set
✅ **Complete**: All user profile data available in ID Token claims
✅ **No Additional Requests**: Faster authentication (one less HTTP call)

## Configuration

### `.env` File
```properties
# Authorization endpoint - Browser redirects (localhost for public access)
OIDC_AUTHORIZATION_URI=http://localhost:9090/realms/raptor/protocol/openid-connect/auth

# Token endpoint - Backend server-to-server call
OIDC_TOKEN_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/token

# UserInfo endpoint - NOT USED (CustomOidcUserService extracts from ID Token)
OIDC_USER_INFO_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/userinfo

# JWK Set endpoint - Backend JWT signature validation
OIDC_JWK_SET_URI=http://host.docker.internal:9090/realms/raptor/protocol/openid-connect/certs
```

### `application.properties`
```properties
# OIDC Provider Configuration - Explicit endpoints (no auto-discovery)
spring.security.oauth2.client.provider.oidc-provider.authorization-uri=${OIDC_AUTHORIZATION_URI}
spring.security.oauth2.client.provider.oidc-provider.token-uri=${OIDC_TOKEN_URI}
spring.security.oauth2.client.provider.oidc-provider.user-info-uri=${OIDC_USER_INFO_URI}
spring.security.oauth2.client.provider.oidc-provider.jwk-set-uri=${OIDC_JWK_SET_URI}
spring.security.oauth2.client.provider.oidc-provider.user-name-attribute=preferred_username
```

### `SecurityConfig.java`
```java
@Configuration
public class SecurityConfig {
    private final CustomOidcUserService customOidcUserService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(customOidcUserService)  // Use custom service
            )
            .successHandler(oauth2SuccessHandler)
        );
        return http.build();
    }
}
```

### `docker-compose.yml`
```yaml
backend:
  extra_hosts:
    - "host.docker.internal:host-gateway"  # Enable container-to-host networking
  environment:
    - OIDC_AUTHORIZATION_URI=${OIDC_AUTHORIZATION_URI}
    - OIDC_TOKEN_URI=${OIDC_TOKEN_URI}
    - OIDC_USER_INFO_URI=${OIDC_USER_INFO_URI}
    - OIDC_JWK_SET_URI=${OIDC_JWK_SET_URI}
```

## Keycloak Configuration

**Frontend URL** (configured via Admin API):
```powershell
# Run: .\configure-keycloak-frontend-url.ps1
# Sets frontendUrl=http://localhost:9090
```

This ensures Keycloak returns consistent issuer claims in all tokens regardless of which hostname is used to access it.

## Testing

1. **Clear browser cookies**: `Ctrl+Shift+Delete`
2. **Navigate to frontend**: `http://localhost:4200`
3. **Click Login button**
4. **Authenticate**: user@raptor.local / Arif@123456789012
5. **Check backend logs**:
   ```bash
   docker logs rap-backend --tail 50 | Select-String -Pattern "CustomOidcUserService|preferred_username|authorities"
   ```

**Expected logs**:
```
Loading OIDC user from ID Token claims (NOT calling UserInfo endpoint)
User preferred_username: user@raptor.local
Extracted authorities: [ROLE_USER, ROLE_ADMIN]
OIDC user loaded successfully: user@raptor.local
```

## Azure Deployment

For Azure deployment, update environment variables to use public Keycloak URL:

```properties
# Azure: Both browser and backend use same public URL
OIDC_AUTHORIZATION_URI=https://keycloak.example.com/realms/raptor/protocol/openid-connect/auth
OIDC_TOKEN_URI=https://keycloak.example.com/realms/raptor/protocol/openid-connect/token
OIDC_JWK_SET_URI=https://keycloak.example.com/realms/raptor/protocol/openid-connect/certs
```

No code changes needed - the CustomOidcUserService works identically whether hostnames match or not.

## Alternative Solutions Attempted

❌ **Mixed hostname endpoints**: 401 UserInfo error
❌ **Unified localhost endpoints**: Backend container can't reach host's localhost
❌ **Auto-discovery with issuer-uri**: Discovery returns localhost URLs, backend can't resolve
❌ **Keycloak frontendUrl only**: Still 401 when calling different hostname

✅ **CustomOidcUserService**: Bypasses UserInfo endpoint entirely, works in all scenarios
