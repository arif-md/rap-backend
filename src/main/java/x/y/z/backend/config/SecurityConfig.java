package x.y.z.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration with Multiple Filter Chains.
 * 
 * This configuration supports:
 * 1. OIDC Authentication for external users (via external OIDC provider)
 * 2. SAML/SSO Authentication for internal users (via Active Directory)
 * 3. Currently DISABLED for development - will be enabled in production
 * 
 * For containerized environments:
 * - Each filter chain handles specific authentication mechanisms
 * - Order matters: More specific chains should come before general ones
 * - This approach is suitable for microservices with multiple auth requirements
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * OIDC Filter Chain for External Users.
     * Order(1) - Processes first for /api/external/** endpoints
     * 
     * CURRENTLY DISABLED - To enable, uncomment oauth2Login() configuration
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oidcFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/external/**") // Apply only to external APIs
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // DISABLED: Change to authenticated() when ready
            )
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
            .cors(cors -> cors.disable()); // Enable CORS as needed

        // TO ENABLE OIDC: Uncomment below and configure application.properties
        /*
        http.oauth2Login(oauth2 -> oauth2
            .loginPage("/login/oidc")
            .defaultSuccessUrl("/api/external/home")
        );
        */

        return http.build();
    }

    /**
     * SAML/SSO Filter Chain for Internal Users (AD/Corporate).
     * Order(2) - Processes for /api/internal/** endpoints
     * 
     * CURRENTLY DISABLED - To enable, configure SAML metadata and uncomment saml2Login()
     */
    @Bean
    @Order(2)
    public SecurityFilterChain samlFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/internal/**") // Apply only to internal APIs
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() // DISABLED: Change to authenticated() when ready
            )
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
            .cors(cors -> cors.disable()); // Enable CORS as needed

        // TO ENABLE SAML: Uncomment below and configure SAML metadata in application.properties
        /*
        http.saml2Login(saml2 -> saml2
            .loginPage("/login/saml")
            .defaultSuccessUrl("/api/internal/home")
        );
        */

        return http.build();
    }

    /**
     * Default Filter Chain for Public APIs and Actuator.
     * Order(3) - Catches all remaining requests
     * 
     * Permits:
     * - Health check endpoints (/api/health, /actuator/health)
     * - Public application CRUD endpoints for testing
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/health", "/api/").permitAll()
                .requestMatchers("/api/applications/**").permitAll() // FOR TESTING - Restrict in production
                .anyRequest().permitAll() // DISABLED: Change to authenticated() for production
            )
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
            .cors(cors -> cors.disable()); // Enable CORS configuration as needed

        return http.build();
    }
}

/*
 * ===========================================================================
 * PRODUCTION ENABLEMENT GUIDE
 * ===========================================================================
 * 
 * 1. OIDC Configuration (External Users):
 *    Add to application.properties:
 *    
 *    spring.security.oauth2.client.registration.oidc.client-id=<your-client-id>
 *    spring.security.oauth2.client.registration.oidc.client-secret=<your-client-secret>
 *    spring.security.oauth2.client.registration.oidc.scope=openid,profile,email
 *    spring.security.oauth2.client.provider.oidc.issuer-uri=https://<your-oidc-provider>/.well-known/openid-configuration
 *    
 *    Then uncomment oauth2Login() in oidcFilterChain()
 *    Change permitAll() to authenticated()
 * 
 * 2. SAML Configuration (Internal Users):
 *    Add to application.properties:
 *    
 *    spring.security.saml2.relyingparty.registration.ad.assertingparty.metadata-uri=https://<ad-server>/FederationMetadata/2007-06/FederationMetadata.xml
 *    spring.security.saml2.relyingparty.registration.ad.entity-id=<your-entity-id>
 *    spring.security.saml2.relyingparty.registration.ad.acs.location={baseUrl}/login/saml2/sso/{registrationId}
 *    
 *    Then uncomment saml2Login() in samlFilterChain()
 *    Change permitAll() to authenticated()
 * 
 * 3. Container Environment Considerations:
 *    - Enable HTTPS/TLS termination at ingress/gateway level
 *    - Configure CORS for frontend origin: http://localhost:4200 (dev), https://your-domain.com (prod)
 *    - Store client secrets in Azure Key Vault, reference via Managed Identity
 *    - Use session management with Redis for distributed sessions (if needed)
 *    - Enable security headers (HSTS, X-Frame-Options, etc.)
 * 
 * 4. Testing Strategy:
 *    - Test OIDC flow: /api/external/test → Should redirect to OIDC provider
 *    - Test SAML flow: /api/internal/test → Should redirect to AD login
 *    - Test public APIs: /api/applications → Should work without auth (for now)
 *    - Verify JWT tokens contain required claims
 *    - Test token refresh and logout flows
 * 
 * ===========================================================================
 */
