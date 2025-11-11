package x.y.z.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Task;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.exception.ResourceNotFoundException;
import x.y.z.backend.handler.ProcessHandler;
import x.y.z.backend.handler.UserHandler;

import java.util.List;

/**
 * ProcessService - Service layer for workflow tasks with BUSINESS LOGIC and TRANSACTION BOUNDARY.
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
public class ProcessService {

    private final ProcessHandler processHandler;
    private final UserHandler userHandler;

    public ProcessService(ProcessHandler processHandler, UserHandler userHandler) {
        this.processHandler = processHandler;
        this.userHandler = userHandler;
    }

    /**
     * Create a new task.
     * BUSINESS LOGIC: Validates required fields, sets defaults.
     */
    public Task createTask(Task task) {
        // Business Rule 1: Set default status if not provided
        if (task.getStatus() == null || task.getStatus().isEmpty()) {
            task.setStatus("PENDING");
        }

        // Business Rule 2: Validate status values
        validateStatus(task.getStatus());

        // Business Rule 3: Validate required fields
        validateRequiredFields(task);

        // Delegate to handler for data access
        return processHandler.insert(task);
    }

    /**
     * Update an existing task.
     * BUSINESS LOGIC: Validates existence, validates status.
     */
    public Task updateTask(Task task) {
        // Business Rule 1: Check if task exists
        Task existing = processHandler.findById(task.getId());
        if (existing == null) {
            throw new ResourceNotFoundException("Task", task.getId());
        }

        // Business Rule 2: Validate status values
        validateStatus(task.getStatus());

        // Business Rule 3: Validate required fields
        validateRequiredFields(task);

        // Delegate to handler for data access
        return processHandler.update(task);
    }

    /**
     * Delete a task by ID.
     * BUSINESS LOGIC: Validates existence.
     */
    public void deleteTask(Long id) {
        // Business Rule 1: Check if task exists
        Task existing = processHandler.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Task", id);
        }

        // Delegate to handler for data access
        processHandler.delete(id);
    }

    /**
     * Get task by ID.
     * BUSINESS LOGIC: Validates existence.
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        Task task = processHandler.findById(id);
        if (task == null) {
            throw new ResourceNotFoundException("Task", id);
        }
        return task;
    }

    /**
     * Get all tasks.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return processHandler.findAll();
    }

    /**
     * Get tasks by status.
     * BUSINESS LOGIC: Validates status value.
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(String status) {
        // Business Rule: Validate status value
        validateStatus(status);
        
        return processHandler.findByStatus(status);
    }

    /**
     * Get tasks by application number.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByApplicationNumber(String applicationNumber) {
        return processHandler.findByApplicationNumber(applicationNumber);
    }

    /**
     * Get total count of tasks.
     * Read-only operation - no business logic needed.
     */
    @Transactional(readOnly = true)
    public long getTaskCount() {
        return processHandler.count();
    }

    /**
     * Get tasks assigned to a specific user with pagination.
     * @param assignedTo The user's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing tasks and pagination metadata
     */
    @Transactional(readOnly = true)
    public PageResponse<Task> getTasksByUser(String assignedTo, int page, int size) {
        // Business Rule: Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            throw new IllegalArgumentException("Assigned to email is required");
        }
        
        // Look up user ID from username/email
        User user = userHandler.findByEmail(assignedTo);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + assignedTo);
        }
        
        // Query tasks by user ID (convert UUID to string for MyBatis)
        return processHandler.findByUserPaginated(user.getId().toString(), page, size);
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
                "Valid values: PENDING, IN_PROGRESS, COMPLETED, CANCELLED"
            );
        }
    }

    /**
     * Check if status is in the allowed list.
     */
    private boolean isValidStatus(String status) {
        return status != null && (
            status.equals("PENDING") || 
            status.equals("IN_PROGRESS") || 
            status.equals("COMPLETED") || 
            status.equals("CANCELLED")
        );
    }

    /**
     * Validate required fields.
     * Business rule: Task and assigned to are mandatory.
     */
    private void validateRequiredFields(Task task) {
        if (task.getTask() == null || task.getTask().trim().isEmpty()) {
            throw new IllegalArgumentException("Task description is required");
        }
        
        if (task.getAssignedTo() == null || task.getAssignedTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Assigned to is required");
        }
    }
}
