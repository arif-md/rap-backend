package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.Application;

import java.util.List;

/**
 * MyBatis Mapper interface for Application entity.
 * Queries are defined in ApplicationMapper.xml
 */
@Mapper
@Repository
public interface ApplicationMapper {

    /**
     * Insert a new application record
     */
    int insert(Application application);

    /**
     * Update an existing application record
     */
    int update(Application application);

    /**
     * Delete an application by ID
     */
    int deleteById(@Param("id") Long id);

    /**
     * Find application by ID
     */
    Application findById(@Param("id") Long id);

    /**
     * Find application by application code (unique)
     */
    Application findByApplicationCode(@Param("applicationCode") String applicationCode);

    /**
     * Find all applications
     */
    List<Application> findAll();

    /**
     * Find applications by status
     */
    List<Application> findByStatus(@Param("status") String status);

    /**
     * Search applications by name pattern (case-insensitive)
     */
    List<Application> searchByName(@Param("namePattern") String namePattern);

    /**
     * Count total applications
     */
    long count();

    /**
     * Check if application code exists
     */
    boolean existsByApplicationCode(@Param("applicationCode") String applicationCode);
}
