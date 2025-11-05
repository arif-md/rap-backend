package x.y.z.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.model.RefreshToken;
import x.y.z.backend.domain.model.Role;
import x.y.z.backend.domain.model.RevokedToken;
import x.y.z.backend.handler.RefreshTokenHandler;
import x.y.z.backend.handler.RevokedTokenHandler;
import x.y.z.backend.handler.UserHandler;
import x.y.z.backend.security.JwtTokenUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JwtTokenService - Service layer for JWT token operations.
 * 
 * Responsibilities:
 * - Generate access tokens (JWT) and refresh tokens
 * - Validate and refresh tokens
 * - Revoke tokens (logout)
 * - Manage token blacklist
 */
@Service
@Transactional
public class JwtTokenService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserHandler userHandler;
    private final RefreshTokenHandler refreshTokenHandler;
    private final RevokedTokenHandler revokedTokenHandler;

    public JwtTokenService(
            JwtTokenUtil jwtTokenUtil,
            UserHandler userHandler,
            RefreshTokenHandler refreshTokenHandler,
            RevokedTokenHandler revokedTokenHandler) {
        
        this.jwtTokenUtil = jwtTokenUtil;
        this.userHandler = userHandler;
        this.refreshTokenHandler = refreshTokenHandler;
        this.revokedTokenHandler = revokedTokenHandler;
    }

    /**
     * Generate access token and refresh token for a user
     * 
     * @param userId User's unique ID
     * @return TokenPair containing access token and refresh token
     */
    public TokenPair generateTokens(UUID userId) {
        // Get user roles
        List<Role> roles = userHandler.findRolesByUserId(userId);
        List<String> roleNames = roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        // Get user email
        String email = userHandler.findById(userId).getEmail();

        // Generate access token (JWT)
        String accessToken = jwtTokenUtil.generateAccessToken(userId, email, roleNames);

        // Generate refresh token (random UUID)
        String refreshToken = jwtTokenUtil.generateRefreshToken();
        String refreshTokenHash = hashToken(refreshToken);

        // Store refresh token in database
        Instant expiresAt = jwtTokenUtil.getRefreshTokenExpiration();
        RefreshToken refreshTokenEntity = new RefreshToken(
                userId,
                refreshTokenHash,
                LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
        );
        refreshTokenHandler.insert(refreshTokenEntity);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Refresh access token using refresh token
     * 
     * @param refreshToken Refresh token string
     * @return New TokenPair with new access token and same refresh token
     * @throws IllegalArgumentException if refresh token is invalid or expired
     */
    public TokenPair refreshAccessToken(String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);

        // Find refresh token in database
        RefreshToken storedToken = refreshTokenHandler.findByTokenHash(refreshTokenHash);
        
        if (storedToken == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // Check if token is revoked
        if (storedToken.getIsRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        // Generate new access token
        UUID userId = storedToken.getUserId();
        List<Role> roles = userHandler.findRolesByUserId(userId);
        List<String> roleNames = roles.stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        String email = userHandler.findById(userId).getEmail();
        String newAccessToken = jwtTokenUtil.generateAccessToken(userId, email, roleNames);

        // Return new access token with same refresh token
        return new TokenPair(newAccessToken, refreshToken);
    }

    /**
     * Validate access token and check if it's revoked
     * 
     * @param accessToken JWT access token
     * @return true if valid and not revoked
     */
    @Transactional(readOnly = true)
    public boolean validateAccessToken(String accessToken) {
        try {
            // Parse and validate token signature and expiration
            jwtTokenUtil.validateToken(accessToken);

            // Check if token is in revocation list
            String jti = jwtTokenUtil.getJtiFromToken(accessToken);
            return !revokedTokenHandler.isRevoked(jti);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Revoke access token (add to blacklist) - used for logout
     * 
     * @param accessToken JWT access token to revoke
     * @param reason Reason for revocation (e.g., "LOGOUT", "SECURITY_BREACH")
     */
    public void revokeAccessToken(String accessToken, String reason) {
        try {
            String jti = jwtTokenUtil.getJtiFromToken(accessToken);
            UUID userId = jwtTokenUtil.getUserIdFromToken(accessToken);
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    jwtTokenUtil.getExpirationFromToken(accessToken).toInstant(),
                    ZoneId.systemDefault()
            );

            RevokedToken revokedToken = new RevokedToken(jti, userId, expiresAt, reason);
            revokedTokenHandler.insert(revokedToken);
            
        } catch (Exception e) {
            // Token might be invalid, but we don't care - just log
            System.err.println("Failed to revoke token: " + e.getMessage());
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     * 
     * @param userId User's unique ID
     */
    public void revokeAllRefreshTokens(UUID userId) {
        refreshTokenHandler.revokeAllByUserId(userId, LocalDateTime.now());
    }

    /**
     * Revoke specific refresh token
     * 
     * @param refreshToken Refresh token string
     */
    public void revokeRefreshToken(String refreshToken) {
        String refreshTokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenHandler.findByTokenHash(refreshTokenHash);
        
        if (storedToken != null && !storedToken.getIsRevoked()) {
            refreshTokenHandler.revoke(storedToken.getId(), LocalDateTime.now());
        }
    }

    /**
     * Cleanup expired tokens (scheduled job)
     * Removes expired refresh tokens and revoked access tokens from database
     */
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenHandler.deleteExpired(now);
        revokedTokenHandler.deleteExpired(now);
    }

    /**
     * Hash token for secure storage (SHA-256)
     * 
     * @param token Token string
     * @return Hexadecimal hash string
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * TokenPair - Data class for access token and refresh token
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
