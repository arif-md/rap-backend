package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.Role;

import java.util.List;

/**
 * MyBatis Mapper interface for Role entity.
 * Queries are defined in RoleMapper.xml
 */
@Mapper
@Repository
public interface RoleMapper {

    /**
     * Insert a new role record
     */
    int insert(Role role);

    /**
     * Update an existing role record
     */
    int update(Role role);

    /**
     * Find role by ID
     */
    Role findById(@Param("id") Long id);

    /**
     * Find role by name
     */
    Role findByName(@Param("roleName") String roleName);

    /**
     * Get all roles
     */
    List<Role> findAll();

    /**
     * Get all roles for a specific user
     */
    List<Role> findByUserId(@Param("userId") Long userId);

    /**
     * Delete a role by ID
     */
    int deleteById(@Param("id") Long id);
}
