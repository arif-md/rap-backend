package x.y.z.backend.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Permit POJO - Domain model representing a permit entity.
 * This is used for the "My Permits" tab in the dashboard.
 * Plain Java object without JPA annotations, used with MyBatis.
 */
public class Permit {
    
    private Long id;
    private String permitNumber;
    private String permitType;
    private String status;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private UUID holderId;  // User ID who holds this permit
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Default constructor
    public Permit() {
    }

    // Constructor for creating new permits
    public Permit(String permitNumber, String permitType, String status,
                  LocalDate issueDate, LocalDate expiryDate, 
                  UUID holderId) {
        this.permitNumber = permitNumber;
        this.permitType = permitType;
        this.status = status;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.holderId = holderId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPermitNumber() {
        return permitNumber;
    }

    public void setPermitNumber(String permitNumber) {
        this.permitNumber = permitNumber;
    }

    public String getPermitType() {
        return permitType;
    }

    public void setPermitType(String permitType) {
        this.permitType = permitType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public UUID getHolderId() {
        return holderId;
    }

    public void setHolderId(UUID holderId) {
        this.holderId = holderId;
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
        return "Permit{" +
                "id=" + id +
                ", permitNumber='" + permitNumber + '\'' +
                ", permitType='" + permitType + '\'' +
                ", status='" + status + '\'' +
                ", issueDate=" + issueDate +
                ", expiryDate=" + expiryDate +
                ", holderId=" + holderId +
                '}';
    }
}
