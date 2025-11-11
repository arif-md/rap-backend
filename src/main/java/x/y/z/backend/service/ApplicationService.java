package x.y.z.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.exception.ResourceNotFoundException;
import x.y.z.backend.handler.ApplicationHandler;

import java.util.List;

/**
 * ApplicationService - Service layer with BUSINESS LOGIC and TRANSACTION BOUNDARY.
 * 
 * Responsibilities:
 * 1. Business Logic: Validation rules, business constraints, orchestration
 * 2. Transaction Management: Ensures atomicity and consistency
 * 3. Orchestrates handlers for data access operations
 * 
 * Pattern: REST Controller → Service (Business Logic + @Transactional) → Handler (Data Access) → MyBatis Mapper
 */
@Service
@Transactional
public class ApplicationService {

    private final ApplicationHandler applicationHandler;

    public ApplicationService(ApplicationHandler applicationHandler) {
        this.applicationHandler = applicationHandler;
    }

    /**
     * Create a new application.
     * BUSINESS LOGIC: Validates unique code, sets defaults, validates status.
     */
    public Application createApplication(Application application) {
        // Business Rule 1: Check if application code already exists
        if (applicationHandler.existsByCode(application.getApplicationCode())) {
            throw new IllegalArgumentException(
                "Application code '" + application.getApplicationCode() + "' already exists"
            );
        }

        // Business Rule 2: Set default status if not provided
        if (application.getStatus() == null || application.getStatus().isEmpty()) {
            application.setStatus("ACTIVE");
        }

        // Business Rule 3: Validate status values
        validateStatus(application.getStatus());

        // Business Rule 4: Validate required fields
        validateRequiredFields(application);

        // Delegate to handler for data access
        return applicationHandler.insert(application);
    }

    /**
     * Update an existing application.
     * BUSINESS LOGIC: Validates existence, prevents code changes, validates status.
     */
    public Application updateApplication(Application application) {
        // Business Rule 1: Check if application exists
        Application existing = applicationHandler.findById(application.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("Application", application.getId());
        }

        // Business Rule 2: Cannot change application code
        if (!existing.getApplicationCode().equals(application.getApplicationCode())) {
            throw new IllegalArgumentException(
                "Cannot change application code from '" + existing.getApplicationCode() + 
                "' to '" + application.getApplicationCode() + "'"
            );
        }

        // Business Rule 3: Validate status values
        validateStatus(application.getStatus());

        // Business Rule 4: Validate required fields
        validateRequiredFields(application);

        // Delegate to handler for data access
        return applicationHandler.update(application);
    }

    /**
     * Delete an application by ID.
     * BUSINESS LOGIC: Validates existence, checks business constraints.
     */
    public void deleteApplication(Long id) {
        // Business Rule 1: Check if application exists
        Application existing = applicationHandler.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Application", id);
        }

        // Business Rule 2: Additional business constraints can be added here
        // Example: Cannot delete if application has active dependencies
        // Example: Can only delete if status is ARCHIVED
        
        // Delegate to handler for data access
        applicationHandler.delete(id);
    }

    /**
     * Get application by ID.
     * BUSINESS LOGIC: Validates existence.
     */
    @Transactional(readOnly = true)
    public Application getApplicationById(Long id) {
        Application application = applicationHandler.findById(id);
        if (application == null) {
            throw new ResourceNotFoundException("Application", id);
        }
        return application;
    }

    /**
     * Get application by unique code.
     * BUSINESS LOGIC: Validates existence.
     */
    @Transactional(readOnly = true)
    public Application getApplicationByCode(String applicationCode) {
        Application application = applicationHandler.findByCode(applicationCode);
        if (application == null) {
            throw new ResourceNotFoundException("Application", applicationCode);
        }
        return application;
    }

    /**
     * Get all applications.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Application> getAllApplications() {
        return applicationHandler.findAll();
    }

    /**
     * Get applications by status.
     * BUSINESS LOGIC: Validates status value.
     */
    @Transactional(readOnly = true)
    public List<Application> getApplicationsByStatus(String status) {
        // Business Rule: Validate status value
        validateStatus(status);
        
        return applicationHandler.findByStatus(status);
    }

    /**
     * Search applications by name pattern.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Application> searchApplicationsByName(String namePattern) {
        // Could add business logic here, e.g., sanitize search pattern
        return applicationHandler.searchByName(namePattern);
    }

    /**
     * Get total count of applications.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public long getApplicationCount() {
        return applicationHandler.count();
    }

    /**
     * Get applications for a specific user with pagination.
     * @param userEmail The user's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing applications and pagination metadata
     */
    @Transactional(readOnly = true)
    public PageResponse<Application> getApplicationsByUser(String userEmail, int page, int size) {
        // Business Rule: Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is required");
        }
        
        return applicationHandler.findByUserPaginated(userEmail, page, size);
    }

    // =========================================================================
    // BUSINESS LOGIC HELPER METHODS
    // =========================================================================

    /**
     * Validate status value against allowed values.
     * This is a business rule that can evolve independently.
     */
    private void validateStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException(
                "Invalid status: '" + status + "'. " +
                "Valid values: ACTIVE, INACTIVE, PENDING, ARCHIVED"
            );
        }
    }

    /**
     * Check if status is in the allowed list.
     * This could be moved to a configuration file or database table.
     */
    private boolean isValidStatus(String status) {
        return status != null && (
            status.equals("ACTIVE") || 
            status.equals("INACTIVE") || 
            status.equals("PENDING") || 
            status.equals("ARCHIVED")
        );
    }

    /**
     * Validate required fields.
     * Business rule: Application name and code are mandatory.
     */
    private void validateRequiredFields(Application application) {
        if (application.getApplicationName() == null || application.getApplicationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Application name is required");
        }
        
        if (application.getApplicationCode() == null || application.getApplicationCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Application code is required");
        }

        // Additional business validations can be added here
        // Example: Application code format validation (e.g., must match pattern)
        // Example: Email format validation for ownerEmail
        // Example: Length constraints
    }
}
