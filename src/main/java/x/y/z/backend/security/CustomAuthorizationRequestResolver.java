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
 * - Example: OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.gov/ns/assurance/ial/2
 * - The parameter name will be: acr_values (converted to lowercase with underscores)
 * - If no OIDC_ADDL_REQ_PARAM_* variables are set, default behavior is used (no extra params)
 * 
 * Example Environment Variables:
 * - OIDC_ADDL_REQ_PARAM_ACR_VALUES=http://idmanagement.gov/ns/assurance/ial/2
 * - OIDC_ADDL_REQ_PARAM_PROMPT=login
 * - OIDC_ADDL_REQ_PARAM_UI_LOCALES=en
 */
@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String PARAM_PREFIX = "oidc.addl.req.param.";
    
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
     * Load additional parameters from environment variables with prefix OIDC_ADDL_REQ_PARAM_
     * Environment variable format: OIDC_ADDL_REQ_PARAM_<PARAM_NAME>=<value>
     * Spring normalizes: OIDC_ADDL_REQ_PARAM_ACR_VALUES -> oidc.addl.req.param.acr.values
     */
    private Map<String, String> loadAdditionalParameters() {
        Map<String, String> params = new HashMap<>();
        
        // Try common parameter names that might be configured
        String[] commonParams = {
            "acr_values", "prompt", "ui_locales", "login_hint", "display", 
            "max_age", "claims", "id_token_hint", "nonce", "response_type"
        };
        
        for (String paramName : commonParams) {
            // Spring Boot normalizes OIDC_ADDL_REQ_PARAM_ACR_VALUES to oidc.addl.req.param.acr.values
            // So we need to convert acr_values -> acr.values for property lookup
            String normalizedParamName = paramName.replace('_', '.');
            String propertyKey = PARAM_PREFIX + normalizedParamName;
            String value = environment.getProperty(propertyKey);
            if (value != null && !value.trim().isEmpty()) {
                params.put(paramName, value);  // Use original name (acr_values) for OAuth2 request
                System.out.println("Loaded OIDC param: " + paramName + " = " + value + " (from property: " + propertyKey + ")");
            }
        }
        
        System.out.println("Total OIDC additional params loaded: " + params.size());
        return params;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    /**
     * Customize the authorization request by adding additional parameters.
     * Parameters are loaded from environment variables with prefix OIDC_ADDL_REQ_PARAM_
     * If no additional parameters are configured, returns the original request unchanged.
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        // If no additional parameters configured, return original request
        if (additionalParams.isEmpty()) {
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
