package x.y.z.backend.domain.model;

import java.time.LocalDateTime;

/**
 * UserRole POJO - Domain model representing the many-to-many relationship
 * between users and roles.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class UserRole {
    
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime grantedAt;
    private String grantedBy;        // Admin who granted the role (for audit)

    // Default constructor
    public UserRole() {
    }

    // Constructor for creating new user-role assignments
    public UserRole(Long userId, Long roleId, String grantedBy) {
        this.userId = userId;
        this.roleId = roleId;
        this.grantedBy = grantedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
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
