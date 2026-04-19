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
 * Configuration via Environment Variables:
 * - Set environment variables with prefix: OIDC_ADDL_REQ_PARAM_
 * - Example: OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.dev/ns/assurance/ial/2
 * - The parameter name will be: acr_values (converted to lowercase with underscores)
 * - If no OIDC_ADDL_REQ_PARAM_* variables are set, default behavior is used (no extra params)
 * 
 * Example Environment Variables:
 * - OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.dev/ns/assurance/ial/2
 * - OIDC_ADDL_REQ_PARAM_PROMPT=login
 * - OIDC_ADDL_REQ_PARAM_UI_LOCALES=en
 */
@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String ENV_VAR_PREFIX = "OIDC_ADDL_REQ_PARAM_";
    private static final String DOT_PREFIX = "oidc.addl.req.param.";
    
    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final Environment environment;
    private final Map<String, String> additionalParams;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            Environment environment) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, 
            "/oauth2/authorization"
        );
        this.environment = environment;
        this.additionalParams = loadAdditionalParameters();
    }
    
    /**
     * Load additional parameters from environment properties.
     * 
     * Tries two key formats to support all property sources:
     * 1. Dot-notation: oidc.addl.req.param.acr.values (Bicep-managed App Config keys)
     * 2. UPPER_CASE:   OIDC_ADDL_REQ_PARAM_ACR_VALUES (env vars, manually-set App Config keys)
     * 
     * Spring's environment.getProperty() applies relaxed binding for
     * SystemEnvironmentPropertySource (env vars) but NOT for BootstrapPropertySource
     * (App Config). We try dot-notation first (canonical), then UPPER_CASE as fallback.
     */
    private Map<String, String> loadAdditionalParameters() {
        Map<String, String> params = new HashMap<>();
        
        System.out.println("=== DEBUG: Checking OIDC Additional Parameters ===");
        
        // Parameter names as they appear in the OAuth2 authorization request
        String[] commonParams = {
            "acr_values", "prompt", "ui_locales", "login_hint", "display", 
            "max_age", "claims", "id_token_hint", "nonce", "response_type"
        };
        
        for (String paramName : commonParams) {
            // Try dot-notation first (matches Bicep-generated App Config keys)
            String dotKey = DOT_PREFIX + paramName.replace('_', '.');
            String value = environment.getProperty(dotKey);
            
            // Fallback to UPPER_CASE (matches env vars and manually-set App Config keys)
            if (value == null) {
                String envVarKey = ENV_VAR_PREFIX + paramName.toUpperCase();
                value = environment.getProperty(envVarKey);
            }
            
            System.out.println("Checking param: " + paramName + " = " + value);
            
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
     * Parameters are loaded from environment variables with prefix OIDC_ADDL_REQ_PARAM_
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
