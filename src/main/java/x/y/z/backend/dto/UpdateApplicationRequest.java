package x.y.z.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing application.
 * Application code cannot be changed (included for validation only).
 */
public class UpdateApplicationRequest {

    @NotBlank(message = "Application name is required")
    @Size(max = 255, message = "Application name must not exceed 255 characters")
    private String applicationName;

    @NotBlank(message = "Application code is required")
    @Size(max = 100, message = "Application code must not exceed 100 characters")
    private String applicationCode;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|PENDING|ARCHIVED)$", message = "Status must be one of: ACTIVE, INACTIVE, PENDING, ARCHIVED")
    private String status;

    @Size(max = 255, message = "Owner name must not exceed 255 characters")
    private String ownerName;

    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Invalid email format")
    @Size(max = 255, message = "Owner email must not exceed 255 characters")
    private String ownerEmail;

    // Constructors
    public UpdateApplicationRequest() {
    }

    public UpdateApplicationRequest(String applicationName, String applicationCode, String description,
                                    String status, String ownerName, String ownerEmail) {
        this.applicationName = applicationName;
        this.applicationCode = applicationCode;
        this.description = description;
        this.status = status;
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

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
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
}
