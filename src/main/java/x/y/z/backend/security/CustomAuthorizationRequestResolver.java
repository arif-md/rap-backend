package x.y.z.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CustomAuthorizationRequestResolver - Customizes OAuth2 authorization requests
 * by adding additional parameters required by specific OIDC providers.
 * 
 * This resolver wraps the default Spring Security resolver and enhances it
 * with custom parameters before sending the authorization request.
 * 
 * Configuration via Azure App Configuration (dot-notation keys):
 *   oidc.addl.req.param.acr.values  → acr_values
 *   oidc.addl.req.param.prompt      → prompt
 *   oidc.addl.req.param.response.type → response_type
 * 
 * Bicep manages these keys in App Config with the 'app:' prefix (e.g.,
 * app:oidc.addl.req.param.acr.values). The Spring Cloud Azure bootstrap
 * strips the prefix, so the resolver looks up the key without it.
 * 
 * Relaxed binding does NOT apply to BootstrapPropertySource lookups, so
 * keys must match exactly in dot-notation.
 * 
 * @see docs/AZURE-PROPERTY-RESOLUTION.md for the full property resolution model
 */
@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String DOT_PREFIX = "oidc.addl.req.param.";
    
    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final Map<String, String> additionalParams;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            Environment environment) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, 
            "/oauth2/authorization"
        );
        this.additionalParams = loadAdditionalParameters(environment);
    }
    
    /**
     * Load additional parameters from environment properties.
     * 
     * Keys are in dot-notation matching Bicep-managed App Config keys:
     *   oidc.addl.req.param.acr.values → acr_values
     *   oidc.addl.req.param.prompt     → prompt
     * 
     * Spring's relaxed binding does NOT apply to BootstrapPropertySource
     * (App Config), so keys must match exactly.
     */
    private Map<String, String> loadAdditionalParameters(Environment environment) {
        Map<String, String> params = new HashMap<>();
        
        System.out.println("=== DEBUG: Checking OIDC Additional Parameters ===");
        
        // Parameter names as they appear in the OAuth2 authorization request
        String[] commonParams = {
            "acr_values", "prompt", "ui_locales", "login_hint", "display", 
            "max_age", "claims", "id_token_hint", "nonce", "response_type"
        };
        
        for (String paramName : commonParams) {
            // Dot-notation matches Bicep-generated App Config keys
            String dotKey = DOT_PREFIX + paramName.replace('_', '.');
            String value = environment.getProperty(dotKey);
            
            System.out.println("Checking param: " + paramName + " (key: " + dotKey + ") = " + value);
            
            if (value != null && !value.trim().isEmpty()) {
                params.put(paramName, value);
                System.out.println("✓ Loaded OIDC param: " + paramName + " = " + value);
            }
        }
        
        System.out.println("Total OIDC additional params loaded: " + params.size());
        System.out.println("=== END DEBUG ===");
        return params;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        String registrationId = extractRegistrationId(request);
        return customizeAuthorizationRequest(authorizationRequest, registrationId);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, clientRegistrationId);
    }

    /**
     * Extract the OAuth2 client registration ID from the request URI.
     * Default pattern: /oauth2/authorization/{registrationId}
     */
    private String extractRegistrationId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/oauth2/authorization/";
        if (uri != null && uri.startsWith(prefix) && uri.length() > prefix.length()) {
            return uri.substring(prefix.length());
        }
        return null;
    }

    /**
     * Customize the authorization request by adding additional parameters.
     * Parameters are loaded from App Config (dot-notation keys under oidc.addl.req.param.*).
     * Only applied to the generic OIDC provider — Azure AD does not need Login.gov-specific
     * parameters like acr_values and would ignore them, but sending them is unnecessary.
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, String registrationId) {
        if (authorizationRequest == null) {
            return null;
        }

        // If no additional parameters configured, return original request
        if (additionalParams.isEmpty()) {
            return authorizationRequest;
        }

        // Only apply OIDC additional params to the external OIDC provider, not Azure AD
        if (AzureAdOidcUserService.REGISTRATION_ID.equals(registrationId)) {
            return authorizationRequest;
        }

        // Create a mutable copy of existing additional parameters
        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());

        // Add configured additional parameters
        additionalParameters.putAll(additionalParams);
        
        // Build and return the customized authorization request
        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .additionalParameters(additionalParameters)
            .build();
    }
}
