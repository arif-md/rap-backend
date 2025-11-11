package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Task;
import x.y.z.backend.repository.mapper.ProcessMapper;

import java.util.List;

/**
 * ProcessHandler - Handles CRUD operations and data access logic for workflow tasks.
 * This handler encapsulates data access operations and calls MyBatis mappers.
 * Business logic should be in the Service layer, not here.
 * 
 * Pattern: REST Controller → Service (Business Logic + @Transactional) → Handler (Data Access) → MyBatis Mapper
 */
@Component
public class ProcessHandler {

    private final ProcessMapper processMapper;

    public ProcessHandler(ProcessMapper processMapper) {
        this.processMapper = processMapper;
    }

    /**
     * Insert a new task record.
     * Pure data access - no business logic.
     */
    public Task insert(Task task) {
        int rowsInserted = processMapper.insert(task);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert task");
        }
        return task; // ID is populated by MyBatis (useGeneratedKeys)
    }

    /**
     * Update an existing task record.
     * Pure data access - no business logic.
     */
    public Task update(Task task) {
        int rowsUpdated = processMapper.update(task);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update task");
        }
        return processMapper.findById(task.getId());
    }

    /**
     * Delete a task by ID.
     * Pure data access - no business logic.
     */
    public void delete(Long id) {
        int rowsDeleted = processMapper.deleteById(id);
        if (rowsDeleted == 0) {
            throw new RuntimeException("Failed to delete task with ID: " + id);
        }
    }

    /**
     * Find task by ID.
     */
    public Task findById(Long id) {
        return processMapper.findById(id);
    }

    /**
     * Find all tasks.
     */
    public List<Task> findAll() {
        return processMapper.findAll();
    }

    /**
     * Find tasks by status.
     */
    public List<Task> findByStatus(String status) {
        return processMapper.findByStatus(status);
    }

    /**
     * Find tasks by application number.
     */
    public List<Task> findByApplicationNumber(String applicationNumber) {
        return processMapper.findByApplicationNumber(applicationNumber);
    }

    /**
     * Count total tasks.
     */
    public long count() {
        return processMapper.count();
    }

    /**
     * Find tasks assigned to a user with pagination.
     * @param assignedTo The user's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing tasks and pagination metadata
     */
    public PageResponse<Task> findByUserPaginated(String assignedTo, int page, int size) {
        int offset = page * size;
        List<Task> tasks = processMapper.findByUserPaginated(assignedTo, offset, size);
        long totalElements = processMapper.countByUser(assignedTo);
        
        return new PageResponse<>(tasks, page, size, totalElements);
    }
}
