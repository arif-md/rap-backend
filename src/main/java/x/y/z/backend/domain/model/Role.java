package x.y.z.backend.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Role POJO - Domain model representing an application role for authorization.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class Role {
    
    private UUID id;
    private String roleName;         // e.g., 'USER', 'ADMIN', 'MANAGER'
    private String description;
    private LocalDateTime createdAt;

    // Default constructor
    public Role() {
    }

    // Constructor for creating new roles
    public Role(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
