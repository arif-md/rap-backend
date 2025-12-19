package x.y.z.backend.domain.model;

import java.time.LocalDateTime;

/**
 * RevokedToken POJO - Domain model representing a revoked JWT access token.
 * Used to maintain a blacklist of invalidated tokens until their natural expiration.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class RevokedToken {
    
    private Long id;
    private String jti;              // JWT ID (unique identifier for the token)
    private Long userId;
    private LocalDateTime expiresAt; // Original expiration of the token
    private LocalDateTime revokedAt;
    private String reason;           // e.g., 'LOGOUT', 'SECURITY_BREACH', 'ADMIN_ACTION'

    // Default constructor
    public RevokedToken() {
    }

    // Constructor for revoking tokens
    public RevokedToken(String jti, Long userId, LocalDateTime expiresAt, String reason) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RevokedToken{" +
                "id=" + id +
                ", jti='" + jti + '\'' +
                ", userId=" + userId +
                ", expiresAt=" + expiresAt +
                ", revokedAt=" + revokedAt +
                ", reason='" + reason + '\'' +
                '}';
    }
}
