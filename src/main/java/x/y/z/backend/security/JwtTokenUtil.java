package x.y.z.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JwtTokenUtil - Utility class for JWT token generation and validation.
 * Handles low-level JWT operations using JJWT library.
 */
@Component
public class JwtTokenUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationDays;
    private final String issuer;

    public JwtTokenUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-minutes:15}") long accessTokenExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays,
            @Value("${jwt.issuer:raptor-app}") String issuer) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.issuer = issuer;
    }

    /**
     * Generate an access token (JWT) for authenticated user
     * 
     * @param userId User's unique ID
     * @param email User's email
     * @param roles List of role names (e.g., ["USER", "ADMIN"])
     * @return Signed JWT token string
     */
    public String generateAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())  // jti (JWT ID) for revocation tracking
                .setSubject(userId.toString())         // User ID as subject
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("email", email)
                .claim("roles", roles)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a refresh token (opaque random token, not JWT)
     * 
     * @return Random UUID token string
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Calculate refresh token expiration time
     * 
     * @return Instant when refresh token should expire
     */
    public Instant getRefreshTokenExpiration() {
        return Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
    }

    /**
     * Validate and parse JWT token
     * 
     * @param token JWT token string
     * @return Claims if valid
     * @throws io.jsonwebtoken.JwtException if invalid or expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract user ID from token
     * 
     * @param token JWT token string
     * @return User ID (Long)
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extract JWT ID (jti) from token for revocation tracking
     * 
     * @param token JWT token string
     * @return JWT ID
     */
    public String getJtiFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getId();
    }

    /**
     * Extract email from token
     * 
     * @param token JWT token string
     * @return Email address
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract roles from token
     * 
     * @param token JWT token string
     * @return List of role names
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Extract expiration time from token
     * 
     * @param token JWT token string
     * @return Expiration date
     */
    public Date getExpirationFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration();
    }

    /**
     * Check if token is expired
     * 
     * @param token JWT token string
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
