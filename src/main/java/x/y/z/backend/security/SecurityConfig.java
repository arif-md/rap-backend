package x.y.z.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig - Spring Security configuration for OIDC authentication and JWT-based authorization.
 * 
 * Security Flow:
 * 1. User initiates login → redirects to OIDC provider
 * 2. OIDC provider authenticates → redirects back to /auth/callback
 * 3. Backend validates OIDC token → creates/updates user → generates JWT
 * 4. Frontend stores JWT in httpOnly cookie
 * 5. All subsequent requests include JWT → validated by JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final CustomOidcUserService customOidcUserService;
    private final CustomAuthorizationRequestResolver authorizationRequestResolver;
    
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
            CustomOidcUserService customOidcUserService,
            CustomAuthorizationRequestResolver authorizationRequestResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.customOidcUserService = customOidcUserService;
        this.authorizationRequestResolver = authorizationRequestResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF - Disabled for stateless JWT (enabled for cookies would require CSRF tokens)
            .csrf(csrf -> csrf.disable())
            
            // Session management - IF_REQUIRED for OAuth2 login flow
            // OAuth2 needs sessions to store authorization state during callback
            // After callback, JWT tokens are used (stateless)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/config/**").permitAll()  // Configuration endpoint for frontend
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Auth endpoints (no JWT required, uses OIDC)
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/callback").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/logout").permitAll()  // Allow logout without authentication
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Login configuration
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(customOidcUserService)  // Use custom OIDC user service
                )
                .successHandler(oauth2SuccessHandler)  // Use custom success handler
                .failureUrl("/auth/login?error=true")
            )
            
            // Disable default logout - we handle it manually in AuthController
            .logout(logout -> logout.disable());

        // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration - allows frontend to make requests to backend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (frontend URLs)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies) only if not using wildcard origins
        // Spring Boot doesn't allow allowCredentials=true with origins="*"
        boolean isWildcardOrigin = Arrays.asList(allowedOrigins).contains("*");
        configuration.setAllowCredentials(!isWildcardOrigin);
        
        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
