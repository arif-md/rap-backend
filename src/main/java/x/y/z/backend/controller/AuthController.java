package x.y.z.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.security.JwtAuthenticationFilter;
import x.y.z.backend.security.JwtTokenUtil;
import x.y.z.backend.service.JwtTokenService;
import x.y.z.backend.service.UserService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuthController - REST Controller for authentication and token management.
 * 
 * Endpoints:
 * - POST /auth/login - Initiates OIDC login (redirects to provider)
 * - GET /auth/callback - Handles OIDC callback and generates JWT tokens
 * - POST /auth/refresh - Refreshes access token using refresh token
 * - POST /auth/logout - Revokes tokens and logs out user
 * - GET /auth/user - Get current authenticated user info
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    
    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    @Value("${jwt.access-token-expiration-minutes:15}")
    private long accessTokenExpirationMinutes;
    
    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    public AuthController(
            UserService userService,
            JwtTokenService jwtTokenService,
            JwtTokenUtil jwtTokenUtil) {
        
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * POST /auth/login
     * Initiates OIDC login flow (handled by Spring Security OAuth2)
     * This endpoint is primarily for documentation - actual redirect is handled by SecurityConfig
     * 
     * @return Redirect info or handled by Spring Security
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Redirecting to OIDC provider...");
        response.put("authorizationUrl", "/oauth2/authorization/oidc-provider");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /auth/callback
     * 
     * NOTE: This endpoint is no longer used. Spring Security's OAuth2 login
     * handles the callback automatically at /login/oauth2/code/{registrationId}.
     * After successful authentication, OAuth2AuthenticationSuccessHandler is invoked
     * to create JWT tokens and redirect to the frontend.
     * 
     * This endpoint is kept for backwards compatibility but should not be called directly.
     */
    @GetMapping("/callback")
    @Deprecated
    public void handleOidcCallback(
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletResponse response) throws IOException {

        // This should not be reached - Spring Security handles OAuth2 callback automatically
        if (oidcUser == null) {
            response.sendRedirect(frontendUrl + "/sign-in?error=no_authentication");
            return;
        }

        try {
            // Create or update user from OIDC
            User user = userService.getOrCreateUserFromOidc(oidcUser);

            // Generate JWT access token and refresh token
            JwtTokenService.TokenPair tokens = jwtTokenService.generateTokens(user.getId());

            // Set access token as httpOnly cookie (15 min expiry)
            Cookie accessTokenCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    (int) (accessTokenExpirationMinutes * 60) // Convert to seconds
            );
            response.addCookie(accessTokenCookie);

            // Set refresh token as httpOnly cookie (7 day expiry)
            Cookie refreshTokenCookie = createCookie(
                    "refresh_token",
                    tokens.getRefreshToken(),
                    (int) (refreshTokenExpirationDays * 24 * 60 * 60) // Convert to seconds
            );
            response.addCookie(refreshTokenCookie);

            // Redirect browser to frontend dashboard
            response.sendRedirect(frontendUrl + "/dashboard");

        } catch (Exception e) {
            // On error, redirect to frontend login with error parameter
            response.sendRedirect(frontendUrl + "/sign-in?error=" + e.getMessage());
        }
    }

    /**
     * POST /auth/refresh
     * Forces OIDC re-authentication when access token expires.
     * 
     * CURRENT BEHAVIOR: Returns requiresReauth=true to force frontend redirect to OIDC provider.
     * This ensures users periodically re-authenticate with OIDC provider, maintaining
     * strong security and compliance with OIDC session policies.
     * 
     * ALTERNATIVE (Silent Refresh): To enable silent token refresh without OIDC interaction,
     * comment out OPTION 1 block and uncomment OPTION 2 block. This is less secure but
     * provides seamless UX for low-risk applications.
     * 
     * @param request HTTP request to extract refresh token cookie
     * @param response HTTP response to set new access token cookie
     * @return Error response requiring OIDC re-authentication
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // OPTION 1: Force OIDC re-authentication (ENABLED - recommended for security)
        // Comment this block to enable silent refresh (see OPTION 2 below)
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Session expired. Please login again.");
        errorResponse.put("requiresReauth", true);
        errorResponse.put("loginUrl", "/auth/login");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        // OPTION 2: Silent refresh (DISABLED - uncomment to enable)
        // This allows seamless session extension without OIDC interaction
        /*
        try {
            // Extract refresh token from cookie
            String refreshToken = extractTokenFromCookie(request, "refresh_token");

            if (refreshToken == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Refresh token not found. Please login again.");
                errorResponse.put("requiresReauth", true);
                errorResponse.put("loginUrl", "/auth/login");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Generate new access token
            JwtTokenService.TokenPair tokens = jwtTokenService.refreshAccessToken(refreshToken);

            // Set new access token as httpOnly cookie
            Cookie accessTokenCookie = createCookie(
                    "access_token",
                    tokens.getAccessToken(),
                    (int) (accessTokenExpirationMinutes * 60)
            );
            response.addCookie(accessTokenCookie);

            // Return success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Token refreshed successfully");
            responseBody.put("expiresIn", accessTokenExpirationMinutes * 60); // seconds

            return ResponseEntity.ok(responseBody);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("requiresReauth", true);
            errorResponse.put("loginUrl", "/auth/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Token refresh failed: " + e.getMessage());
            errorResponse.put("requiresReauth", true);
            errorResponse.put("requiresReauth", true);
            errorResponse.put("loginUrl", "/auth/login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        */
    }

    /**
     * POST /auth/logout
     * Logs out user by revoking access token and refresh token.
     * Clears authentication cookies.
     * 
     * @param request HTTP request to extract tokens
     * @param response HTTP response to clear cookies
     * @return Success response
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // Extract tokens from cookies
            String accessToken = extractTokenFromCookie(request, "access_token");
            String refreshToken = extractTokenFromCookie(request, "refresh_token");

            // Revoke access token (add to blacklist)
            if (accessToken != null) {
                jwtTokenService.revokeAccessToken(accessToken, "LOGOUT");
            }

            // Revoke refresh token
            if (refreshToken != null) {
                jwtTokenService.revokeRefreshToken(refreshToken);
            }

            // Clear cookies
            Cookie accessTokenCookie = createCookie("access_token", "", 0);
            Cookie refreshTokenCookie = createCookie("refresh_token", "", 0);
            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);

            // Return success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Logout successful");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /auth/user
     * Get current authenticated user information from JWT token.
     * 
     * @param request HTTP request to extract access token
     * @return User information
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {

        try {
            // Extract access token from cookie
            String accessToken = extractTokenFromCookie(request, "access_token");

            if (accessToken == null || !jwtTokenService.validateAccessToken(accessToken)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Extract user info from token
            UUID userId = jwtTokenUtil.getUserIdFromToken(accessToken);
            String email = jwtTokenUtil.getEmailFromToken(accessToken);
            var roles = jwtTokenUtil.getRolesFromToken(accessToken);

            // Get full user details from database
            User user = userService.findById(userId);

            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Return user info directly (not wrapped in "user" field)
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId().toString());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("oidcSubject", user.getOidcSubject());
            userInfo.put("roles", roles);
            userInfo.put("isActive", user.getIsActive());
            userInfo.put("lastLoginAt", user.getLastLoginAt());
            userInfo.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get user info: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /auth/check
     * Check if current session is valid (for frontend to decide if OIDC re-auth is needed)
     * 
     * This endpoint helps frontend determine:
     * - Is access token valid? → Continue normal operation
     * - Is access token expired but refresh token valid? → Call /auth/refresh OR redirect to OIDC
     * - Are both tokens invalid/expired? → Redirect to OIDC login
     * 
     * @param request HTTP request to extract tokens
     * @return Session status
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkSession(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String accessToken = extractTokenFromCookie(request, "access_token");
            String refreshToken = extractTokenFromCookie(request, "refresh_token");

            // Check access token
            boolean accessTokenValid = accessToken != null && jwtTokenService.validateAccessToken(accessToken);

            if (accessTokenValid) {
                // Extract user info from token
                UUID userId = jwtTokenUtil.getUserIdFromToken(accessToken);
                String email = jwtTokenUtil.getEmailFromToken(accessToken);
                var roles = jwtTokenUtil.getRolesFromToken(accessToken);

                // Get full user details from database
                User user = userService.findById(userId);

                if (user == null) {
                    response.put("authenticated", false);
                    response.put("error", "User not found");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }

                response.put("authenticated", true);
                response.put("accessTokenValid", true);
                response.put("requiresReauth", false);
                response.put("user", Map.of(
                        "id", user.getId().toString(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "oidcSubject", user.getOidcSubject(),
                        "roles", roles,
                        "isActive", user.getIsActive(),
                        "lastLoginAt", user.getLastLoginAt(),
                        "createdAt", user.getCreatedAt()
                ));
                return ResponseEntity.ok(response);
            }

            // Access token invalid - check refresh token
            if (refreshToken != null) {
                try {
                    // Just check if refresh token exists and is valid (don't issue new token yet)
                    jwtTokenService.refreshAccessToken(refreshToken); // This validates refresh token
                    
                    response.put("authenticated", false);
                    response.put("accessTokenValid", false);
                    response.put("refreshTokenValid", true);
                    response.put("requiresReauth", true); // Force OIDC re-auth as per your requirement
                    response.put("message", "Access token expired. Please re-authenticate.");
                    response.put("loginUrl", "/auth/login");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                    
                } catch (Exception e) {
                    // Refresh token also invalid
                    response.put("authenticated", false);
                    response.put("accessTokenValid", false);
                    response.put("refreshTokenValid", false);
                    response.put("requiresReauth", true);
                    response.put("message", "Session expired. Please login.");
                    response.put("loginUrl", "/auth/login");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }

            // No tokens at all
            response.put("authenticated", false);
            response.put("accessTokenValid", false);
            response.put("refreshTokenValid", false);
            response.put("requiresReauth", true);
            response.put("message", "Not authenticated. Please login.");
            response.put("loginUrl", "/auth/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper: Create httpOnly cookie
     */
    private Cookie createCookie(String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        // cookie.setSameSite("Strict"); // Enable in production for CSRF protection
        return cookie;
    }

    /**
     * Helper: Extract token from cookie
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
}
