package x.y.z.backend.domain.model;

import java.time.LocalDateTime;

/**
 * Application POJO - Domain model representing an application entity.
 * This is a plain Java object without JPA annotations, used with MyBatis.
 */
public class Application extends TaskForm {
    
    private String applicationName;
    private String description;
    // Not a stored column - populated on read via WORKFLOW_APP_ASSOC.STATUS_CD joined to GENERIC_REF_TBL.
    private String status;
    private String ownerName;
    private String ownerEmail;
    private String universityName; // Transient - populated via JOIN
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Default constructor
    public Application() {
    }

    // Constructor for creating new applications
    public Application(String applicationName, String applicationCode, String description,
                      String ownerName, String ownerEmail) {
        this.applicationName = applicationName;
        this.applicationCode = applicationCode;
        this.description = description;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    // Getters and Setters
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + this.id +
                ", applicationName='" + applicationName + '\'' +
                ", applicationCode='" + applicationCode + '\'' +
                ", status='" + status + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
