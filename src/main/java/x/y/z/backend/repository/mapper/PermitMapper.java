package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.Permit;

import java.util.List;

/**
 * MyBatis Mapper interface for Permit entity.
 * Queries are defined in PermitMapper.xml
 */
@Mapper
@Repository
public interface PermitMapper {

    /**
     * Insert a new permit record
     */
    int insert(Permit permit);

    /**
     * Update an existing permit record
     */
    int update(Permit permit);

    /**
     * Delete a permit by ID
     */
    int deleteById(@Param("id") Long id);

    /**
     * Find permit by ID
     */
    Permit findById(@Param("id") Long id);

    /**
     * Find permit by permit number (unique)
     */
    Permit findByPermitNumber(@Param("permitNumber") String permitNumber);

    /**
     * Find all permits
     */
    List<Permit> findAll();

    /**
     * Find permits by holder ID with pagination
     * @param holderId The permit holder's user ID
     * @param offset The starting record index
     * @param limit The maximum number of records to return
     */
    List<Permit> findByUserPaginated(
        @Param("holderId") String holderId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    /**
     * Count total permits for a specific holder
     * @param holderId The permit holder's user ID
     */
    long countByUser(@Param("holderId") String holderId);

    /**
     * Find permits by status
     */
    List<Permit> findByStatus(@Param("status") String status);

    /**
     * Find permits by type
     */
    List<Permit> findByType(@Param("permitType") String permitType);

    /**
     * Count total permits
     */
    long count();

    /**
     * Check if permit number exists
     */
    boolean existsByPermitNumber(@Param("permitNumber") String permitNumber);
}
