package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.Task;

import java.util.List;

/**
 * MyBatis Mapper interface for Task entity (workflow tasks).
 * Queries are defined in ProcessMapper.xml
 */
@Mapper
@Repository
public interface ProcessMapper {

    /**
     * Insert a new task record
     */
    int insert(Task task);

    /**
     * Update an existing task record
     */
    int update(Task task);

    /**
     * Delete a task by ID
     */
    int deleteById(@Param("id") Long id);

    /**
     * Find task by ID
     */
    Task findById(@Param("id") Long id);

    /**
     * Find all tasks
     */
    List<Task> findAll();

    /**
     * Find tasks assigned to a specific user with pagination
     * @param assignedTo The user email address
     * @param offset The starting record index
     * @param limit The maximum number of records to return
     */
    List<Task> findByUserPaginated(
        @Param("assignedTo") String assignedTo,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    /**
     * Count total tasks assigned to a specific user
     * @param assignedTo The user email address
     */
    long countByUser(@Param("assignedTo") String assignedTo);

    /**
     * Find tasks by status
     */
    List<Task> findByStatus(@Param("status") String status);

    /**
     * Find tasks by application number
     */
    List<Task> findByApplicationNumber(@Param("applicationNumber") String applicationNumber);

    /**
     * Count total tasks
     */
    long count();
}
