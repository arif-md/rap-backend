package x.y.z.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Permit;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.exception.ResourceNotFoundException;
import x.y.z.backend.handler.PermitHandler;
import x.y.z.backend.handler.UserHandler;

import java.util.List;

/**
 * PermitService - Service layer for permits with BUSINESS LOGIC and TRANSACTION BOUNDARY.
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
public class PermitService {

    private final PermitHandler permitHandler;
    private final UserHandler userHandler;

    public PermitService(PermitHandler permitHandler, UserHandler userHandler) {
        this.permitHandler = permitHandler;
        this.userHandler = userHandler;
    }

    /**
     * Create a new permit.
     * BUSINESS LOGIC: Validates unique permit number, sets defaults, validates status.
     */
    public Permit createPermit(Permit permit) {
        // Business Rule 1: Check if permit number already exists
        if (permitHandler.existsByPermitNumber(permit.getPermitNumber())) {
            throw new IllegalArgumentException(
                "Permit number '" + permit.getPermitNumber() + "' already exists"
            );
        }

        // Business Rule 2: Set default status if not provided
        if (permit.getStatus() == null || permit.getStatus().isEmpty()) {
            permit.setStatus("ACTIVE");
        }

        // Business Rule 3: Validate status values
        validateStatus(permit.getStatus());

        // Business Rule 4: Validate required fields
        validateRequiredFields(permit);

        // Business Rule 5: Validate dates (expiry must be after issue)
        validateDates(permit);

        // Delegate to handler for data access
        return permitHandler.insert(permit);
    }

    /**
     * Update an existing permit.
     * BUSINESS LOGIC: Validates existence, prevents permit number changes, validates status.
     */
    public Permit updatePermit(Permit permit) {
        // Business Rule 1: Check if permit exists
        Permit existing = permitHandler.findById(permit.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("Permit", permit.getId());
        }

        // Business Rule 2: Cannot change permit number
        if (!existing.getPermitNumber().equals(permit.getPermitNumber())) {
            throw new IllegalArgumentException(
                "Cannot change permit number from '" + existing.getPermitNumber() + 
                "' to '" + permit.getPermitNumber() + "'"
            );
        }

        // Business Rule 3: Validate status values
        validateStatus(permit.getStatus());

        // Business Rule 4: Validate required fields
        validateRequiredFields(permit);

        // Business Rule 5: Validate dates
        validateDates(permit);

        // Delegate to handler for data access
        return permitHandler.update(permit);
    }

    /**
     * Delete a permit by ID.
     * BUSINESS LOGIC: Validates existence.
     */
    public void deletePermit(Long id) {
        // Business Rule 1: Check if permit exists
        Permit existing = permitHandler.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Permit", id);
        }

        // Delegate to handler for data access
        permitHandler.delete(id);
    }

    /**
     * Get permit by ID.
     * BUSINESS LOGIC: Validates existence.
     */
    @Transactional(readOnly = true)
    public Permit getPermitById(Long id) {
        Permit permit = permitHandler.findById(id);
        if (permit == null) {
            throw new ResourceNotFoundException("Permit", id);
        }
        return permit;
    }

    /**
     * Get permit by permit number.
     * BUSINESS LOGIC: Validates existence.
     */
    @Transactional(readOnly = true)
    public Permit getPermitByNumber(String permitNumber) {
        Permit permit = permitHandler.findByPermitNumber(permitNumber);
        if (permit == null) {
            throw new ResourceNotFoundException("Permit", permitNumber);
        }
        return permit;
    }

    /**
     * Get all permits.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Permit> getAllPermits() {
        return permitHandler.findAll();
    }

    /**
     * Get permits by status.
     * BUSINESS LOGIC: Validates status value.
     */
    @Transactional(readOnly = true)
    public List<Permit> getPermitsByStatus(String status) {
        // Business Rule: Validate status value
        validateStatus(status);
        
        return permitHandler.findByStatus(status);
    }

    /**
     * Get permits by type.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Permit> getPermitsByType(String permitType) {
        return permitHandler.findByType(permitType);
    }

    /**
     * Get total count of permits.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public long getPermitCount() {
        return permitHandler.count();
    }

    /**
     * Get permits for a specific user with pagination.
     * @param holderEmail The permit holder's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing permits and pagination metadata
     */
    @Transactional(readOnly = true)
    public PageResponse<Permit> getPermitsByUser(String holderEmail, int page, int size) {
        // Business Rule: Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (holderEmail == null || holderEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Holder email is required");
        }
        
        // Look up user ID from email
        User user = userHandler.findByEmail(holderEmail);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + holderEmail);
        }
        
        // Query permits by user ID
        return permitHandler.findByUserPaginated(user.getId().toString(), page, size);
    }

    // =========================================================================
    // BUSINESS LOGIC HELPER METHODS
    // =========================================================================

    /**
     * Validate status value against allowed values.
     */
    private void validateStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException(
                "Invalid status: '" + status + "'. " +
                "Valid values: ACTIVE, EXPIRED, SUSPENDED, REVOKED"
            );
        }
    }

    /**
     * Check if status is in the allowed list.
     */
    private boolean isValidStatus(String status) {
        return status != null && (
            status.equals("ACTIVE") || 
            status.equals("EXPIRED") || 
            status.equals("SUSPENDED") || 
            status.equals("REVOKED")
        );
    }

    /**
     * Validate required fields.
     */
    private void validateRequiredFields(Permit permit) {
        if (permit.getPermitNumber() == null || permit.getPermitNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Permit number is required");
        }
        
        if (permit.getPermitType() == null || permit.getPermitType().trim().isEmpty()) {
            throw new IllegalArgumentException("Permit type is required");
        }

        if (permit.getHolderId() == null) {
            throw new IllegalArgumentException("Holder ID is required");
        }
    }

    /**
     * Validate dates (expiry must be after issue).
     */
    private void validateDates(Permit permit) {
        if (permit.getIssueDate() != null && permit.getExpiryDate() != null) {
            if (permit.getExpiryDate().isBefore(permit.getIssueDate())) {
                throw new IllegalArgumentException("Expiry date must be after issue date");
            }
        }
    }
}
