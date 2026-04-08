package x.y.z.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

/**
 * SecurityConfig - Spring Security configuration for dual authentication:
 * 1. OIDC Provider (external users) - Custom OIDC provider (e.g., Keycloak, Identity Sandbox)
 * 2. Azure Entra ID SSO (internal users) - Azure AD for internal staff
 * 
 * Security Flow:
 * 1. User initiates login → redirects to OIDC provider or Azure AD
 * 2. Provider authenticates → redirects back to /login/oauth2/code/{registrationId}
 * 3. Backend validates token → creates/updates user → generates JWT
 * 4. Frontend stores JWT in httpOnly cookie
 * 5. All subsequent requests include JWT → validated by JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;
    private final CustomOidcUserService customOidcUserService;
    private final AzureAdOidcUserService azureAdOidcUserService;
    private final CustomAuthorizationRequestResolver authorizationRequestResolver;
    private final Environment environment;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2FailureHandler,
            CustomOidcUserService customOidcUserService,
            AzureAdOidcUserService azureAdOidcUserService,
            CustomAuthorizationRequestResolver authorizationRequestResolver,
            Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        this.customOidcUserService = customOidcUserService;
        this.azureAdOidcUserService = azureAdOidcUserService;
        this.authorizationRequestResolver = authorizationRequestResolver;
        this.environment = environment;
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
                
                // Auth endpoints - these handle their own authentication internally
                // (extract JWT from cookie, validate, return proper 401 JSON if invalid).
                // They MUST be in permitAll() so Spring Security doesn't intercept
                // unauthenticated requests with a 302 redirect to the OIDC login page,
                // which would break frontend XHR calls (CORS block on redirect).
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/sso-login").permitAll()
                .requestMatchers("/auth/user").permitAll()     // Handles auth internally via JWT cookie
                .requestMatchers("/auth/check").permitAll()    // Handles auth internally via JWT cookie
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/logout").permitAll()
                
                // OAuth2 authorization endpoints (Spring Security auto-registers these)
                .requestMatchers("/oauth2/authorization/**").permitAll()
                .requestMatchers("/login/oauth2/code/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Login configuration - supports both OIDC provider and Azure AD
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/auth/login")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(authorizationRequestResolver)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(new DelegatingOidcUserService(customOidcUserService, azureAdOidcUserService))
                )
                .successHandler(oauth2SuccessHandler)  // Use custom success handler
                .failureHandler(oauth2FailureHandler)   // Redirects to frontend with error details
            )
            
            // Disable default logout - we handle it manually in AuthController
            .logout(logout -> logout.disable());

        // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration - allows frontend to make requests to backend.
     * Reads cors.allowed-origins from the Environment on each request so that
     * changes pushed via App Configuration refresh are picked up without restart.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration() {
            @Override
            public CorsConfiguration applyPermitDefaultValues() {
                return this;
            }

            private String[] resolveOrigins() {
                String raw = environment.getProperty("cors.allowed-origins", "http://localhost:4200");
                return raw.split(",");
            }

            @Override
            public java.util.List<String> getAllowedOrigins() {
                return Arrays.asList(resolveOrigins());
            }

            @Override
            public java.util.List<String> getAllowedMethods() {
                return Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            }

            @Override
            public java.util.List<String> getAllowedHeaders() {
                return Arrays.asList("*");
            }

            @Override
            public Boolean getAllowCredentials() {
                return !Arrays.asList(resolveOrigins()).contains("*");
            }

            @Override
            public java.util.List<String> getExposedHeaders() {
                return Arrays.asList("Authorization", "Set-Cookie");
            }

            @Override
            public Long getMaxAge() {
                return 3600L;
            }
        });
        return source;
    }
}
