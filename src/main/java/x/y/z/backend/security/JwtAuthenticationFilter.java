package x.y.z.backend.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import x.y.z.backend.config.CurrentUser;
import x.y.z.backend.service.JwtTokenService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JwtAuthenticationFilter - Intercepts every HTTP request to validate JWT token.
 * 
 * Flow:
 * 1. Extract JWT from httpOnly cookie (access_token)
 * 2. Validate token signature and expiration
 * 3. Check if token is revoked (blacklist)
 * 4. Extract user info and roles from token
 * 5. Set Spring Security authentication context
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, JwtTokenService jwtTokenService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT from httpOnly cookie
            String jwt = extractJwtFromCookie(request);

            if (jwt != null && jwtTokenService.validateAccessToken(jwt)) {
                // Extract user info from token
                Long userId = jwtTokenUtil.getUserIdFromToken(jwt);
                String email = jwtTokenUtil.getEmailFromToken(jwt);
                List<String> roles = jwtTokenUtil.getRolesFromToken(jwt);

                // Convert roles to Spring Security authorities
                List<GrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, email, roles),
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtException e) {
            // Invalid token - log and continue without authentication
            logger.warn("Invalid JWT token: " + e.getMessage());
        } catch (Exception e) {
            // Unexpected error - log and continue
            logger.error("JWT authentication error: " + e.getMessage(), e);
        }

        // Continue with filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from httpOnly cookie
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    /**
     * UserPrincipal - Represents authenticated user in Spring Security context
     */
    public static class UserPrincipal implements CurrentUser {
        private final Long userId;
        private final String email;
        private final List<String> roles;

        public UserPrincipal(Long userId, String email, List<String> roles) {
            this.userId = userId;
            this.email = email;
            this.roles = roles;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public List<String> getRoles() {
            return roles;
        }

        @Override
        public String toString() {
            return "UserPrincipal{" +
                    "userId=" + userId +
                    ", email='" + email + '\'' +
                    ", roles=" + roles +
                    '}';
        }
    }
}
