package x.y.z.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2AuthenticationFailureHandler — redirects to the frontend login page
 * with a machine-readable error code so the UI can display a friendly message.
 *
 * Error codes forwarded:
 *   unauthorized_email  — email domain not allowed (AzureAdOidcUserService)
 *   user_not_found      — user not provisioned in DB (AzureAdOidcUserService)
 *   user_deactivated    — account disabled (AzureAdOidcUserService)
 *   <provider code>     — upstream provider error (e.g. invalid_grant, server_error)
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String errorCode = "server_error";

        if (exception instanceof OAuth2AuthenticationException oauth2Ex) {
            errorCode = oauth2Ex.getError().getErrorCode();
            logger.warn("OAuth2 authentication failure — code: {}, description: {}",
                    errorCode,
                    oauth2Ex.getError().getDescription());
        } else {
            logger.warn("Authentication failure: {}", exception.getMessage());
        }

        String redirectUrl = frontendUrl + "/sign-in?error="
                + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);

        logger.info("Redirecting to frontend login with error: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
