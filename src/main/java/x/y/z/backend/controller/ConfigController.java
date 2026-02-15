package x.y.z.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Controller
 * 
 * <p>Provides runtime configuration to the frontend application.</p>
 * 
 * <h3>Dual Configuration Strategy:</h3>
 * <ul>
 *   <li><strong>Container/Azure Deployment:</strong> Configuration is injected at container 
 *       startup via docker-entrypoint.sh into assets/runtime-config.json (static file)</li>
 *   <li><strong>Local Development (ng serve):</strong> Configuration is served dynamically 
 *       via this REST API endpoint</li>
 * </ul>
 * 
 * <p>The frontend {@code AppConfigService} implements a fallback pattern:</p>
 * <ol>
 *   <li>Try to load assets/runtime-config.json (container environments)</li>
 *   <li>If not found (404), fall back to /api/config/environmentProperties (local dev)</li>
 * </ol>
 * 
 * <h3>Future Optimization:</h3>
 * <p>This controller could be conditionally loaded only in local development environments
 * using Spring profiles to reduce footprint in production:</p>
 * <pre>
 * {@code @Profile({"local", "dev", "!prod"})}
 * public class ConfigController { ... }
 * </pre>
 * 
 * <p>This would prevent the controller from being instantiated in Azure/container deployments
 * where it's not needed since the frontend uses runtime-config.json instead.</p>
 * 
 * @see org.springframework.context.annotation.Profile
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${spring.application.name:backend}")
    private String applicationName;

    @Value("${jwt.access-token-expiration-minutes:15}")
    private int jwtAccessTokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private int jwtRefreshTokenExpirationDays;

    @Value("${APP_ENV:local}")
    private String appEnv;

    @Value("${APP_ENV_NAME:Local}")
    private String appEnvName;

    /**
     * GET /api/config/environmentProperties
     * 
     * <p>Returns runtime configuration for the frontend application in local development.</p>
     * 
     * <p>The response format matches the frontend {@code EnvironmentProps} TypeScript interface,
     * ensuring compatibility with the frontend configuration model.</p>
     * 
     * <h3>Configuration Sources:</h3>
     * <ul>
     *   <li>JWT settings: From application.properties or environment variables</li>
     *   <li>Environment name: From APP_ENV_NAME environment variable</li>
     *   <li>Build version: Hardcoded as "dev-local" for local development</li>
     * </ul>
     * 
     * <h3>Security:</h3>
     * <p>This endpoint is publicly accessible (permitAll in SecurityConfig) because the
     * frontend needs to load configuration before authentication. It only exposes
     * non-sensitive configuration values like timeout durations and environment names.</p>
     * 
     * @return Map of configuration properties matching EnvironmentProps interface
     */
    @GetMapping("/environmentProperties")
    public Map<String, Object> getEnvironmentProperties() {
        Map<String, Object> config = new HashMap<>();
        
        // Application environment info
        config.put("appEnv", appEnv);
        config.put("appEnvName", appEnvName);
        
        // JWT configuration - frontend needs these for session timer
        config.put("jwtAccessTokenExpirationMinutes", jwtAccessTokenExpirationMinutes);
        config.put("jwtRefreshTokenExpirationDays", jwtRefreshTokenExpirationDays);
        
        // Build version (can be enhanced with Git info from build)
        config.put("buildVersion", "dev-local");
        
        // API base URL (frontend determines this from its own location)
        config.put("apiBaseUrl", "");
        
        return config;
    }
}
