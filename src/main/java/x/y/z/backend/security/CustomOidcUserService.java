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

import x.y.z.backend.domain.model.Role;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.handler.UserHandler;

import java.time.LocalDateTime;
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
 * <p>
 * On first authentication, this service automatically creates a user record in the database,
 * ensuring that user-dependent endpoints (workflow tasks, permits) can function correctly.
 */
@Component
public class CustomOidcUserService extends OidcUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);
    
    private final UserHandler userHandler;
    
    public CustomOidcUserService(UserHandler userHandler) {
        this.userHandler = userHandler;
    }
    
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
        
        // Sync user to database (create if doesn't exist, update last login if exists)
        // Also sync roles to database
        syncUserToDatabase(claims, authorities);
        
        // Determine which claim to use as the principal name
        // Different OIDC providers return different claims
        String nameAttributeKey = determineNameAttributeKey(claims);
        logger.info("Using '{}' as principal name attribute", nameAttributeKey);
        
        // Create OidcUser from ID Token claims only (no UserInfo call)
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, nameAttributeKey);
        
        logger.info("OIDC user loaded successfully: {} (subject: {})", 
            claims.get(nameAttributeKey), 
            oidcUser.getSubject());
        
        return oidcUser;
    }
    
    /**
     * Determine which claim to use as the principal name attribute.
     * Tries preferred_username first (Keycloak standard), falls back to email, then sub.
     */
    private String determineNameAttributeKey(Map<String, Object> claims) {
        if (claims.containsKey("preferred_username") && claims.get("preferred_username") != null) {
            return "preferred_username";
        } else if (claims.containsKey("email") && claims.get("email") != null) {
            return "email";
        } else {
            // sub (subject) is required by OIDC spec, always present
            return "sub";
        }
    }
    
    /**
     * Sync OIDC user to local database.
     * Creates user if doesn't exist, updates last login timestamp if exists.
     * Also syncs roles to database.
     */
    private void syncUserToDatabase(Map<String, Object> claims, Set<GrantedAuthority> authorities) {
        String oidcSubject = (String) claims.get("sub");
        String email = (String) claims.get("email");
        String name = (String) claims.get("name");
        String preferredUsername = (String) claims.get("preferred_username");
        
        // Use name if available, fall back to preferred_username, then email
        String fullName = name != null ? name : (preferredUsername != null ? preferredUsername : email);
        
        try {
            // Check if user already exists
            User existingUser = userHandler.findByOidcSubject(oidcSubject);
            
            User user;
            if (existingUser != null) {
                // Update last login timestamp
                logger.info("User exists in database: {} (ID: {})", existingUser.getEmail(), existingUser.getId());
                userHandler.updateLastLogin(existingUser.getId(), LocalDateTime.now());
                logger.debug("Updated last login for user: {}", existingUser.getEmail());
                user = existingUser;
            } else {
                // Create new user
                User newUser = new User(oidcSubject, email, fullName);
                user = userHandler.insert(newUser);
                logger.info("Created new user in database: {} (ID: {})", user.getEmail(), user.getId());
            }
            
            // Sync roles to database
            syncRolesToDatabase(user, authorities);
            
        } catch (Exception e) {
            logger.error("Failed to sync user to database: {}", oidcSubject, e);
            // Don't throw exception - allow authentication to proceed even if DB sync fails
            // This ensures authentication continues to work even if database is temporarily unavailable
        }
    }
    
    /**
     * Synchronize user roles to database.
     * Clears existing roles and inserts new ones from authorities.
     */
    private void syncRolesToDatabase(User user, Set<GrantedAuthority> authorities) {
        try {
            logger.debug("Syncing roles to database for user: {}", user.getEmail());
            
            // Clear existing roles
            userHandler.clearUserRoles(user.getId());
            logger.debug("Cleared existing roles for user: {}", user.getEmail());
            
            // Add each role from authorities
            for (GrantedAuthority authority : authorities) {
                String authorityName = authority.getAuthority();
                // Remove "ROLE_" prefix to get the role name
                String roleName = authorityName.startsWith("ROLE_") ? 
                    authorityName.substring(5) : authorityName;
                
                Role role = userHandler.findRoleByName(roleName);
                if (role != null) {
                    userHandler.assignRole(user.getId(), role.getId(), "SYSTEM");
                    logger.debug("Assigned role {} to user {}", roleName, user.getEmail());
                } else {
                    logger.warn("Role not found in database: {}", roleName);
                }
            }
            
            logger.info("Successfully synced {} roles to database for user: {}", authorities.size(), user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to sync roles for user: {}", user.getId(), e);
            // Don't throw exception - allow authentication to proceed even if role sync fails
        }
    }
    
    /**
     * Extract authorities from ID Token claims.
     * Looks for realm_access.roles and resource_access claims from Keycloak.
     */
    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add default authenticated authority
        //authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
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
        
        // If no roles from OIDC provider, default to ROLE_EXTERNAL_USER
        if (authorities.size() == 0){// && authorities.stream().allMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            logger.info("No specific roles from OIDC provider, defaulting to ROLE_EXTERNAL_USER");
            authorities.add(new SimpleGrantedAuthority("ROLE_EXTERNAL_USER"));
        }
        
        return authorities;
    }
}
