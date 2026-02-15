package x.y.z.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.service.JwtTokenService;
import x.y.z.backend.service.UserService;

import java.io.IOException;

/**
 * Custom OAuth2 authentication success handler.
 * 
 * This handler is invoked after successful OAuth2/OIDC authentication from either:
 * 1. External OIDC provider (oidc-provider registration) → redirects to /auth-callback
 * 2. Azure Entra ID SSO (azure-ad registration) → redirects to /auth-callback?provider=azure-ad
 * 
 * It:
 * 1. Clears any existing authentication tokens (requirement: treat as new login)
 * 2. Creates or updates the user in the database from OIDC claims
 * 3. Generates JWT access and refresh tokens
 * 4. Sets tokens as HttpOnly cookies
 * 5. Redirects to the frontend application with provider info
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    public OAuth2AuthenticationSuccessHandler(
            UserService userService,
            JwtTokenService jwtTokenService,
            JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        logger.info("OAuth2 authentication successful for user: {}", authentication.getName());

        try {
            // Determine which provider was used (oidc-provider vs azure-ad)
            String registrationId = "unknown";
            if (authentication instanceof OAuth2AuthenticationToken) {
                registrationId = ((OAuth2AuthenticationToken) authentication)
                        .getAuthorizedClientRegistrationId();
            }
            logger.info("Authentication provider registration: {}", registrationId);
            boolean isInternalProvider = DelegatingOidcUserService.isInternalProvider(registrationId);

            // REQUIREMENT 3: Clear any existing tokens before creating new ones
            // This ensures that switching between external/internal login always starts fresh
            clearExistingTokens(request, response);

            // Extract OIDC user from authentication
            if (!(authentication.getPrincipal() instanceof OidcUser)) {
                logger.error("Authentication principal is not an OidcUser: {}", 
                    authentication.getPrincipal().getClass().getName());
                response.sendRedirect(frontendUrl + "/login?error=invalid_authentication");
                return;
            }

            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            logger.debug("OIDC user subject: {}", oidcUser.getSubject());

            // Create or update user from OIDC claims
            User user = userService.getOrCreateUserFromOidc(oidcUser);
            logger.info("User created/updated: {} (ID: {})", user.getEmail(), user.getId());

            // Generate JWT tokens
            JwtTokenService.TokenPair tokens = jwtTokenService.generateTokens(user.getId());
            logger.info("JWT tokens generated for user ID: {}. JWT roles from DB: {}", 
                user.getId(), 
                jwtTokenUtil.getRolesFromToken(tokens.getAccessToken()));

            // Set access token cookie (15 minutes)
            Cookie accessTokenCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    (int) (accessTokenExpirationMinutes * 60)
            );
            response.addCookie(accessTokenCookie);
            logger.debug("Access token cookie set");

            // Set refresh token cookie (7 days)
            Cookie refreshTokenCookie = createCookie(
                    "refresh_token",
                    tokens.getRefreshToken(),
                    (int) (refreshTokenExpirationDays * 24 * 60 * 60)
            );
            response.addCookie(refreshTokenCookie);
            logger.debug("Refresh token cookie set");

            // Store OIDC ID token for logout (same expiration as refresh token)
            String idToken = oidcUser.getIdToken().getTokenValue();
            Cookie idTokenCookie = createCookie(
                    "id_token",
                    idToken,
                    (int) (refreshTokenExpirationDays * 24 * 60 * 60)
            );
            response.addCookie(idTokenCookie);
            logger.debug("ID token cookie set for OIDC logout");

            // Store auth provider registration ID for provider-aware logout
            // This allows the logout endpoint to redirect to the correct provider's end-session URL
            Cookie authProviderCookie = createCookie(
                    "auth_provider",
                    registrationId,
                    (int) (refreshTokenExpirationDays * 24 * 60 * 60)
            );
            response.addCookie(authProviderCookie);
            logger.debug("Auth provider cookie set: {}", registrationId);

            // Redirect to frontend auth callback component with provider info
            // Frontend uses provider parameter to determine which dashboard to show
            String redirectUrl;
            if (isInternalProvider) {
                redirectUrl = frontendUrl + "/auth-callback?provider=" + registrationId;
                logger.info("Internal provider login ({}) - redirecting to: {}", registrationId, redirectUrl);
            } else {
                redirectUrl = frontendUrl + "/auth-callback";
                logger.info("OIDC provider login - redirecting to: {}", redirectUrl);
            }
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("Error during OAuth2 authentication success handling", e);
            response.sendRedirect(frontendUrl + "/login?error=authentication_failed");
        }
    }

    /**
     * Clear any existing authentication tokens.
     * This ensures a clean state when switching between external/internal login.
     */
    private void clearExistingTokens(HttpServletRequest request, HttpServletResponse response) {
        // Extract and revoke existing tokens
        String existingAccessToken = extractTokenFromCookie(request, "access_token");
        String existingRefreshToken = extractTokenFromCookie(request, "refresh_token");

        if (existingAccessToken != null) {
            try {
                jwtTokenService.revokeAccessToken(existingAccessToken, "NEW_LOGIN");
                logger.debug("Revoked existing access token before new login");
            } catch (Exception e) {
                logger.warn("Failed to revoke existing access token: {}", e.getMessage());
            }
        }

        if (existingRefreshToken != null) {
            try {
                jwtTokenService.revokeRefreshToken(existingRefreshToken);
                logger.debug("Revoked existing refresh token before new login");
            } catch (Exception e) {
                logger.warn("Failed to revoke existing refresh token: {}", e.getMessage());
            }
        }

        // Clear existing cookies
        response.addCookie(createCookie("access_token", "", 0));
        response.addCookie(createCookie("refresh_token", "", 0));
        response.addCookie(createCookie("id_token", "", 0));
        response.addCookie(createCookie("auth_provider", "", 0));
        logger.debug("Cleared existing auth cookies before new login");
    }

    /**
     * Extract token value from a named cookie.
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Creates an HttpOnly, secure cookie for JWT tokens.
     */
    private Cookie createCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}