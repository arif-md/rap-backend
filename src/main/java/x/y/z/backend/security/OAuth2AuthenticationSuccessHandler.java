package x.y.z.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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
 * This handler is invoked after successful OAuth2/OIDC authentication.
 * It:
 * 1. Creates or updates the user in the database from OIDC claims
 * 2. Generates JWT access and refresh tokens
 * 3. Sets tokens as HttpOnly cookies
 * 4. Redirects to the frontend application
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    public OAuth2AuthenticationSuccessHandler(
            UserService userService,
            JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        logger.info("OAuth2 authentication successful for user: {}", authentication.getName());

        try {
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
            logger.debug("JWT tokens generated for user ID: {}", user.getId());

            // Set access token cookie (15 minutes)
            Cookie accessTokenCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    (int) (accessTokenExpirationMinutes * 60) // Convert to seconds
            );
            response.addCookie(accessTokenCookie);
            logger.debug("Access token cookie set");

            // Set refresh token cookie (7 days)
            Cookie refreshTokenCookie = createCookie(
                    "refresh_token",
                    tokens.getRefreshToken(),
                    (int) (refreshTokenExpirationDays * 24 * 60 * 60) // Convert to seconds
            );
            response.addCookie(refreshTokenCookie);
            logger.debug("Refresh token cookie set");

            // Redirect to frontend auth callback component
            // This allows the frontend to fetch user details and handle navigation
            String redirectUrl = frontendUrl + "/auth-callback";
            logger.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            logger.error("Error during OAuth2 authentication success handling", e);
            response.sendRedirect(frontendUrl + "/login?error=authentication_failed");
        }
    }

    /**
     * Creates an HttpOnly, secure cookie for JWT tokens.
     *
     * @param name Cookie name
     * @param value Cookie value (JWT token)
     * @param maxAgeSeconds Cookie expiration time in seconds
     * @return Configured cookie
     */
    private Cookie createCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);  // Prevent JavaScript access (XSS protection)
        cookie.setSecure(false);    // Set to true in production with HTTPS
        cookie.setPath("/");        // Available for all paths
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setDomain("localhost"); // Set domain to allow cross-port cookie sharing
        cookie.setAttribute("SameSite", "Lax");  // CSRF protection
        return cookie;
    }
}
