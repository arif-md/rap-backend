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
 * Azure AD OIDC User Service for internal (SSO) users.
 * <p>
 * Handles authentication via Azure Entra ID. Internal users are differentiated
 * from external users (OIDC provider) by being assigned ROLE_INTERNAL_USER
 * instead of ROLE_EXTERNAL_USER.
 * <p>
 * Azure AD token claims differ from Keycloak:
 * - Roles come from "roles" claim (app roles) or "groups" claim (group membership)
 * - preferred_username is typically the UPN (user@domain.com)
 * - name is the display name
 * - email may or may not be present (use preferred_username as fallback)
 */
@Component
public class AzureAdOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(AzureAdOidcUserService.class);
    
    /** Registration ID for Azure AD in Spring Security OAuth2 config */
    public static final String REGISTRATION_ID = "azure-ad";

    private final UserHandler userHandler;

    public AzureAdOidcUserService(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("Loading Azure AD user from ID Token claims (SSO internal user)");

        // Get ID Token (already validated by Spring Security)
        var idToken = userRequest.getIdToken();
        Map<String, Object> claims = idToken.getClaims();

        logger.debug("Azure AD ID Token claims: {}", claims.keySet());
        logger.info("User sub (subject): {}", claims.get("sub"));
        logger.info("User preferred_username (UPN): {}", claims.get("preferred_username"));
        logger.info("User email: {}", claims.get("email"));
        logger.info("User name: {}", claims.get("name"));

        // Extract roles/authorities from Azure AD token
        Set<GrantedAuthority> authorities = extractAuthorities(claims);
        logger.info("Extracted authorities: {}", authorities);

        // Sync user to database (create if doesn't exist, update last login if exists)
        syncUserToDatabase(claims, authorities);

        // Determine name attribute key
        String nameAttributeKey = determineNameAttributeKey(claims);
        logger.info("Using '{}' as principal name attribute", nameAttributeKey);

        // Create OidcUser from ID Token claims
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, nameAttributeKey);

        logger.info("Azure AD user loaded successfully: {} (subject: {})",
                claims.get(nameAttributeKey),
                oidcUser.getSubject());

        return oidcUser;
    }

    /**
     * Determine which claim to use as the principal name attribute.
     * Azure AD typically provides preferred_username (UPN), name, and email.
     */
    private String determineNameAttributeKey(Map<String, Object> claims) {
        if (claims.containsKey("preferred_username") && claims.get("preferred_username") != null) {
            return "preferred_username";
        } else if (claims.containsKey("email") && claims.get("email") != null) {
            return "email";
        } else {
            return "sub";
        }
    }

    /**
     * Sync Azure AD user to local database.
     * Creates user if doesn't exist, updates last login if exists.
     * Internal users are flagged differently from external users.
     */
    private void syncUserToDatabase(Map<String, Object> claims, Set<GrantedAuthority> authorities) {
        String oidcSubject = (String) claims.get("sub");
        String email = (String) claims.get("email");
        String name = (String) claims.get("name");
        String preferredUsername = (String) claims.get("preferred_username");

        // Azure AD: email may not be in claims; use preferred_username (UPN) as fallback
        if (email == null || email.isEmpty()) {
            email = preferredUsername;
        }

        // Use name if available, fall back to preferred_username, then email
        String fullName = name != null ? name : (preferredUsername != null ? preferredUsername : email);

        try {
            User existingUser = userHandler.findByOidcSubject(oidcSubject);

            User user;
            if (existingUser != null) {
                logger.info("Azure AD user exists in database: {} (ID: {})", existingUser.getEmail(), existingUser.getId());
                userHandler.updateLastLogin(existingUser.getId(), LocalDateTime.now());
                user = existingUser;
            } else {
                // Create new internal user
                User newUser = new User(oidcSubject, email, fullName);
                user = userHandler.insert(newUser);
                logger.info("Created new Azure AD (internal) user in database: {} (ID: {})", user.getEmail(), user.getId());
            }

            // Sync roles to database
            syncRolesToDatabase(user, authorities);

        } catch (Exception e) {
            logger.error("Failed to sync Azure AD user to database: {}", oidcSubject, e);
        }
    }

    /**
     * Synchronize user roles to database.
     */
    private void syncRolesToDatabase(User user, Set<GrantedAuthority> authorities) {
        try {
            logger.debug("Syncing roles to database for Azure AD user: {}", user.getEmail());

            userHandler.clearUserRoles(user.getId());

            for (GrantedAuthority authority : authorities) {
                String authorityName = authority.getAuthority();
                String roleName = authorityName.startsWith("ROLE_") ?
                        authorityName.substring(5) : authorityName;

                Role role = userHandler.findRoleByName(roleName);
                if (role != null) {
                    userHandler.assignRole(user.getId(), role.getId(), "SYSTEM");
                    logger.debug("Assigned role {} to Azure AD user {}", roleName, user.getEmail());
                } else {
                    logger.warn("Role not found in database: {}", roleName);
                }
            }

            logger.info("Successfully synced {} roles for Azure AD user: {}", authorities.size(), user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to sync roles for Azure AD user: {}", user.getId(), e);
        }
    }

    /**
     * Extract authorities from Azure AD ID Token claims.
     * <p>
     * Azure AD provides roles in different claim formats:
     * - "roles": App roles assigned in Azure AD Enterprise Application
     * - "groups": Azure AD group object IDs (if configured in token)
     * - "wids": Azure AD built-in role template IDs
     * <p>
     * Internal users always get ROLE_INTERNAL_USER as a baseline.
     */
    @SuppressWarnings("unchecked")
    private Set<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Always assign ROLE_INTERNAL_USER for Azure AD SSO users
        authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL_USER"));
        logger.debug("Added baseline role: ROLE_INTERNAL_USER (Azure AD SSO user)");

        // Extract app roles from "roles" claim
        if (claims.containsKey("roles")) {
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                List<String> roles = (List<String>) rolesObj;
                roles.forEach(role -> {
                    String normalizedRole = "ROLE_" + role.toUpperCase().replace(" ", "_").replace("-", "_");
                    authorities.add(new SimpleGrantedAuthority(normalizedRole));
                    logger.debug("Added Azure AD app role: {}", normalizedRole);
                });
            }
        }

        // Extract group-based roles from "groups" claim (optional)
        if (claims.containsKey("groups")) {
            Object groupsObj = claims.get("groups");
            if (groupsObj instanceof List) {
                List<String> groups = (List<String>) groupsObj;
                groups.forEach(groupId -> {
                    // Group IDs are GUIDs - map them to role names if needed
                    // For now, just log them. Specific mapping can be configured.
                    logger.debug("Azure AD group membership: {}", groupId);
                });
            }
        }

        return authorities;
    }
}
