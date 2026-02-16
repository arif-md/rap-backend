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
import java.util.Map;
import java.util.Set;

/**
 * Custom OIDC User Service for external users.
 * <p>
 * Extracts user information from ID Token claims only (no UserInfo endpoint call).
 * This avoids hostname mismatch issues between browser and backend in local development.
 * <p>
 * Role extraction from token claims has been removed. External users are assigned
 * a default ROLE_EXTERNAL_USER authority. Actual roles for JWT generation are read
 * from the database by JwtTokenService.
 * <p>
 * User provisioning (create on first login) is handled by
 * {@link OAuth2AuthenticationSuccessHandler} via {@code UserService.getOrCreateUserFromOidc()}.
 */
@Component
public class CustomOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("Loading external OIDC user from ID Token claims");

        var idToken = userRequest.getIdToken();
        Map<String, Object> claims = idToken.getClaims();

        logger.info("External user login: sub={}, email={}, preferred_username={}",
                claims.get("sub"), claims.get("email"), claims.get("preferred_username"));

        // Default authority for external users — actual JWT roles come from DB
        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_EXTERNAL_USER"));

        String nameAttributeKey = determineNameAttributeKey(claims);
        logger.info("Using '{}' as principal name attribute", nameAttributeKey);

        OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, nameAttributeKey);

        logger.info("External OIDC user loaded: {} (subject: {})",
                claims.get(nameAttributeKey), oidcUser.getSubject());

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
        }
        return "sub";
    }
}

