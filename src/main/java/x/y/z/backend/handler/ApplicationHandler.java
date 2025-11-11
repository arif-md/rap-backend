package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.repository.mapper.ApplicationMapper;

import java.util.List;

/**
 * ApplicationHandler - Handles CRUD operations and data access logic.
 * This handler encapsulates data access operations and calls MyBatis mappers.
 * Business logic should be in the Service layer, not here.
 * 
 * Pattern: REST Controller → Service (Business Logic + @Transactional) → Handler (Data Access) → MyBatis Mapper
 */
@Component
public class ApplicationHandler {

    private final ApplicationMapper applicationMapper;

    public ApplicationHandler(ApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    /**
     * Insert a new application record.
     * Pure data access - no business logic.
     */
    public Application insert(Application application) {
        int rowsInserted = applicationMapper.insert(application);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert application");
        }
        return application; // ID is populated by MyBatis (useGeneratedKeys)
    }

    /**
     * Update an existing application record.
     * Pure data access - no business logic.
     */
    public Application update(Application application) {
        int rowsUpdated = applicationMapper.update(application);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update application");
        }
        return applicationMapper.findById(application.getId());
    }

    /**
     * Delete an application by ID.
     * Pure data access - no business logic.
     */
    public void delete(Long id) {
        int rowsDeleted = applicationMapper.deleteById(id);
        if (rowsDeleted == 0) {
            throw new RuntimeException("Failed to delete application with ID: " + id);
        }
    }

    /**
     * Find application by ID.
     */
    public Application findById(Long id) {
        return applicationMapper.findById(id);
    }

    /**
     * Find application by unique code.
     */
    public Application findByCode(String applicationCode) {
        return applicationMapper.findByApplicationCode(applicationCode);
    }

    /**
     * Find all applications.
     */
    public List<Application> findAll() {
        return applicationMapper.findAll();
    }

    /**
     * Find applications by status.
     */
    public List<Application> findByStatus(String status) {
        return applicationMapper.findByStatus(status);
    }

    /**
     * Search applications by name pattern.
     */
    public List<Application> searchByName(String namePattern) {
        return applicationMapper.searchByName(namePattern);
    }

    /**
     * Count total applications.
     */
    public long count() {
        return applicationMapper.count();
    }

    /**
     * Check if application code exists.
     */
    public boolean existsByCode(String applicationCode) {
        return applicationMapper.existsByApplicationCode(applicationCode);
    }

    /**
     * Find applications by user email with pagination.
     * @param userEmail The user's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing applications and pagination metadata
     */
    public PageResponse<Application> findByUserPaginated(String userEmail, int page, int size) {
        int offset = page * size;
        List<Application> applications = applicationMapper.findByUserPaginated(userEmail, offset, size);
        long totalElements = applicationMapper.countByUser(userEmail);
        
        return new PageResponse<>(applications, page, size, totalElements);
    }
}
