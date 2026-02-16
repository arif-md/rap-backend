package x.y.z.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
 * Simplified authentication flow:
 * 1. Extract email from Azure AD token claims
 * 2. Validate email ends with @nexgeninc.com
 * 3. Verify user exists in the database (no auto-provisioning)
 * 4. Load roles from the database (NOT from token claims)
 * 5. Update last login timestamp
 * <p>
 * No role extraction from Azure AD tokens. No user/role sync to database.
 * Internal users and their roles must be pre-provisioned by an administrator.
 */
@Component
public class AzureAdOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(AzureAdOidcUserService.class);

    /** Registration ID for Azure AD in Spring Security OAuth2 config */
    public static final String REGISTRATION_ID = "azure-ad";

    /** Only emails ending with this domain are authorized for internal login */
    private static final String ALLOWED_EMAIL_DOMAIN = "@nexgeninc.com";

    private final UserHandler userHandler;

    public AzureAdOidcUserService(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("Loading Azure AD user (internal SSO)");

        var idToken = userRequest.getIdToken();
        Map<String, Object> claims = idToken.getClaims();

        // 1. Extract email from token claims
        String email = extractEmail(claims);
        logger.info("Azure AD login attempt for email: {}", email);

        // 2. Validate email domain (@nexgeninc.com only)
        if (email == null || !email.toLowerCase().endsWith(ALLOWED_EMAIL_DOMAIN)) {
            logger.warn("Azure AD login rejected: email '{}' does not end with {}", email, ALLOWED_EMAIL_DOMAIN);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unauthorized_email",
                            "Only @nexgeninc.com email addresses are authorized for internal login", null));
        }

        // 3. Verify user exists in database (no auto-provisioning)
        User user = userHandler.findByEmail(email);
        if (user == null) {
            logger.warn("Azure AD login rejected: user '{}' not found in database", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_not_found",
                            "User not authorized. Please contact your administrator.", null));
        }

        if (!user.getIsActive()) {
            logger.warn("Azure AD login rejected: user '{}' is deactivated", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_deactivated",
                            "Your account has been deactivated. Please contact your administrator.", null));
        }

        // 4. Load roles from database (NOT from token claims)
        List<Role> dbRoles = userHandler.findRolesByUserId(user.getId());
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Role role : dbRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName().toUpperCase()));
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL_USER"));
            logger.info("No roles in DB for user {}, defaulting to ROLE_INTERNAL_USER", email);
        }
        logger.info("Loaded {} role(s) from DB for user {}: {}", authorities.size(), email, authorities);

        // 5. Update last login timestamp
        userHandler.updateLastLogin(user.getId(), LocalDateTime.now());

        // 6. Return OidcUser with DB-sourced authorities
        String nameAttributeKey = determineNameAttributeKey(claims);
        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, nameAttributeKey);

        logger.info("Azure AD user authenticated successfully: {} (ID: {})", email, user.getId());
        return oidcUser;
    }

    /**
     * Extract email from Azure AD token claims.
     * Azure AD: email may not be in claims; use preferred_username (UPN) as fallback.
     */
    private String extractEmail(Map<String, Object> claims) {
        String email = (String) claims.get("email");
        if (email == null || email.isEmpty()) {
            email = (String) claims.get("preferred_username");
        }
        return email;
    }

    /**
     * Determine which claim to use as the principal name attribute.
     */
    private String determineNameAttributeKey(Map<String, Object> claims) {
        if (claims.containsKey("preferred_username") && claims.get("preferred_username") != null) {
            return "preferred_username";
        } else if (claims.containsKey("email") && claims.get("email") != null) {
            return "email";
        }
        return "sub";
    }
}
