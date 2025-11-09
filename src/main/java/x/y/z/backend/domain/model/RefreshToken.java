package x.y.z.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken POJO - Domain model representing a JWT refresh token.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class RefreshToken {
    
    private UUID id;
    private UUID userId;
    private String tokenHash;        // SHA-256 hash of the refresh token (for security)
    
    // Token Lifecycle
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    
    // Security Metadata
    private String ipAddress;
    private String userAgent;
    
    // Revocation
    private Boolean isRevoked;
    private LocalDateTime revokedAt;
    private String revokedReason;

    // Default constructor
    public RefreshToken() {
    }

    // Constructor for creating new refresh tokens
    public RefreshToken(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.isRevoked = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public void setIsRevoked(Boolean isRevoked) {
        this.isRevoked = isRevoked;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", tokenHash='" + tokenHash + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", lastUsedAt=" + lastUsedAt +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", isRevoked=" + isRevoked +
                ", revokedAt=" + revokedAt +
                ", revokedReason='" + revokedReason + '\'' +
                '}';
    }
}
