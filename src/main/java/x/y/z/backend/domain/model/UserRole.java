package x.y.z.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserRole POJO - Domain model representing the many-to-many relationship
 * between users and roles.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class UserRole {
    
    private UUID id;
    private UUID userId;
    private UUID roleId;
    private LocalDateTime grantedAt;
    private String grantedBy;        // Admin who granted the role (for audit)

    // Default constructor
    public UserRole() {
    }

    // Constructor for creating new user-role assignments
    public UserRole(UUID userId, UUID roleId, String grantedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.grantedBy = grantedBy;
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

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", userId=" + userId +
                ", roleId=" + roleId +
                ", grantedAt=" + grantedAt +
                ", grantedBy='" + grantedBy + '\'' +
                '}';
    }
}
