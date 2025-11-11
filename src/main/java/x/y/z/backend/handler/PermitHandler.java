package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Permit;
import x.y.z.backend.repository.mapper.PermitMapper;

import java.util.List;

/**
 * PermitHandler - Handles CRUD operations and data access logic for permits.
 * This handler encapsulates data access operations and calls MyBatis mappers.
 * Business logic should be in the Service layer, not here.
 * 
 * Pattern: REST Controller → Service (Business Logic + @Transactional) → Handler (Data Access) → MyBatis Mapper
 */
@Component
public class PermitHandler {

    private final PermitMapper permitMapper;

    public PermitHandler(PermitMapper permitMapper) {
        this.permitMapper = permitMapper;
    }

    /**
     * Insert a new permit record.
     * Pure data access - no business logic.
     */
    public Permit insert(Permit permit) {
        int rowsInserted = permitMapper.insert(permit);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert permit");
        }
        return permit; // ID is populated by MyBatis (useGeneratedKeys)
    }

    /**
     * Update an existing permit record.
     * Pure data access - no business logic.
     */
    public Permit update(Permit permit) {
        int rowsUpdated = permitMapper.update(permit);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update permit");
        }
        return permitMapper.findById(permit.getId());
    }

    /**
     * Delete a permit by ID.
     * Pure data access - no business logic.
     */
    public void delete(Long id) {
        int rowsDeleted = permitMapper.deleteById(id);
        if (rowsDeleted == 0) {
            throw new RuntimeException("Failed to delete permit with ID: " + id);
        }
    }

    /**
     * Find permit by ID.
     */
    public Permit findById(Long id) {
        return permitMapper.findById(id);
    }

    /**
     * Find permit by permit number.
     */
    public Permit findByPermitNumber(String permitNumber) {
        return permitMapper.findByPermitNumber(permitNumber);
    }

    /**
     * Find all permits.
     */
    public List<Permit> findAll() {
        return permitMapper.findAll();
    }

    /**
     * Find permits by status.
     */
    public List<Permit> findByStatus(String status) {
        return permitMapper.findByStatus(status);
    }

    /**
     * Find permits by type.
     */
    public List<Permit> findByType(String permitType) {
        return permitMapper.findByType(permitType);
    }

    /**
     * Count total permits.
     */
    public long count() {
        return permitMapper.count();
    }

    /**
     * Check if permit number exists.
     */
    public boolean existsByPermitNumber(String permitNumber) {
        return permitMapper.existsByPermitNumber(permitNumber);
    }

    /**
     * Find permits for a user with pagination.
     * @param holderEmail The permit holder's email address
     * @param page The page number (0-indexed)
     * @param size The number of items per page
     * @return PageResponse containing permits and pagination metadata
     */
    public PageResponse<Permit> findByUserPaginated(String holderEmail, int page, int size) {
        int offset = page * size;
        List<Permit> permits = permitMapper.findByUserPaginated(holderEmail, offset, size);
        long totalElements = permitMapper.countByUser(holderEmail);
        
        return new PageResponse<>(permits, page, size, totalElements);
    }
}
