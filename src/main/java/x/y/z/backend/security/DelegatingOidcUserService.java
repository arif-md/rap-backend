package x.y.z.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * DelegatingOidcUserService - Routes OIDC user loading to the appropriate
 * service based on the OAuth2 client registration ID.
 * <p>
 * Spring Security's oauth2Login().userInfoEndpoint().oidcUserService() only accepts
 * a single OidcUserService. This delegating service inspects the registration ID
 * from the OidcUserRequest and forwards to:
 * <ul>
 *   <li>{@link CustomOidcUserService} for "oidc-provider" (external users)</li>
 *   <li>{@link AzureAdOidcUserService} for "azure-ad" (internal SSO users)</li>
 *   <li>{@link AzureAdOidcUserService} for "keycloak-internal" (internal users via local Keycloak)</li>
 * </ul>
 */
public class DelegatingOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingOidcUserService.class);

    /** Registration ID for Keycloak-based internal users (offline local dev fallback) */
    public static final String KEYCLOAK_INTERNAL_REGISTRATION_ID = "keycloak-internal";

    private final CustomOidcUserService customOidcUserService;
    private final AzureAdOidcUserService azureAdOidcUserService;

    public DelegatingOidcUserService(
            CustomOidcUserService customOidcUserService,
            AzureAdOidcUserService azureAdOidcUserService) {
        this.customOidcUserService = customOidcUserService;
        this.azureAdOidcUserService = azureAdOidcUserService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        logger.info("Delegating OIDC user loading for registration: {}", registrationId);

        if (isInternalProvider(registrationId)) {
            logger.info("Routing to AzureAdOidcUserService (internal user) for registration: {}", registrationId);
            return azureAdOidcUserService.loadUser(userRequest);
        } else {
            logger.info("Routing to CustomOidcUserService (external OIDC)");
            return customOidcUserService.loadUser(userRequest);
        }
    }

    /**
     * Check if a registration ID corresponds to an internal user provider.
     * Internal providers assign ROLE_INTERNAL_USER instead of ROLE_EXTERNAL_USER.
     * 
     * @param registrationId the OAuth2 client registration ID
     * @return true if the provider should be treated as internal
     */
    public static boolean isInternalProvider(String registrationId) {
        return AzureAdOidcUserService.REGISTRATION_ID.equals(registrationId)
                || KEYCLOAK_INTERNAL_REGISTRATION_ID.equals(registrationId);
    }
}
