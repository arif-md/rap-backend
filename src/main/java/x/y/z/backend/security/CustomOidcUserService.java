package x.y.z.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom OIDC User Service that extracts user information from ID Token claims only.
 * <p>
 * This avoids calling Keycloak's UserInfo endpoint, which solves the hostname mismatch issue:
 * - Browser flow uses localhost:9090 (tokens issued with iss=http://localhost:9090)
 * - Backend container must call host.docker.internal:9090
 * - Keycloak rejects UserInfo requests when hostname doesn't match token issuer
 * <p>
 * Solution: Extract all user data from ID Token claims (which are already validated via JWK Set)
 * This works locally AND in Azure where backend/browser can use same public URL.
 */
@Component
public class CustomOidcUserService extends OidcUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);
    
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("Loading OIDC user from ID Token claims (NOT calling UserInfo endpoint)");
        
        // Get ID Token (already validated by Spring Security)
        var idToken = userRequest.getIdToken();
        Map<String, Object> claims = idToken.getClaims();
        
        logger.debug("ID Token claims: {}", claims.keySet());
        logger.info("User sub (subject): {}", claims.get("sub"));
        logger.info("User preferred_username: {}", claims.get("preferred_username"));
        logger.info("User email: {}", claims.get("email"));
        
        // Extract roles/authorities from ID Token
        Set<GrantedAuthority> authorities = extractAuthorities(claims);
        logger.info("Extracted authorities: {}", authorities);
        
        // Create OidcUser from ID Token claims only (no UserInfo call)
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, "preferred_username");
        
        logger.info("OIDC user loaded successfully: {} ({})", 
            oidcUser.getPreferredUsername(), 
            oidcUser.getSubject());
        
        return oidcUser;
    }
    
    /**
     * Extract authorities from ID Token claims.
     * Looks for realm_access.roles and resource_access claims from Keycloak.
     */
    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add default authenticated authority
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Extract realm roles from ID Token
        if (claims.containsKey("realm_access")) {
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                roles.forEach(role -> {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                    logger.debug("Added realm role: ROLE_{}", role.toUpperCase());
                });
            }
        }
        
        // Extract client roles from ID Token
        if (claims.containsKey("resource_access")) {
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            resourceAccess.forEach((client, access) -> {
                if (access instanceof Map) {
                    Map<String, Object> clientAccess = (Map<String, Object>) access;
                    if (clientAccess.containsKey("roles")) {
                        List<String> roles = (List<String>) clientAccess.get("roles");
                        roles.forEach(role -> {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                            logger.debug("Added client role from {}: ROLE_{}", client, role.toUpperCase());
                        });
                    }
                }
            });
        }
        
        return authorities;
    }
}
