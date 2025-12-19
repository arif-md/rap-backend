package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.UserRole;

import java.util.List;

/**
 * MyBatis Mapper interface for UserRole entity (many-to-many relationship).
 * Queries are defined in UserRoleMapper.xml
 */
@Mapper
@Repository
public interface UserRoleMapper {

    /**
     * Insert a new user-role assignment
     */
    int insert(UserRole userRole);

    /**
     * Find user-role assignment by ID
     */
    UserRole findById(@Param("id") Long id);

    /**
     * Get all role assignments for a specific user
     */
    List<UserRole> findByUserId(@Param("userId") Long userId);

    /**
     * Get all user assignments for a specific role
     */
    List<UserRole> findByRoleId(@Param("roleId") Long roleId);

    /**
     * Check if a user has a specific role
     */
    boolean hasRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * Delete a specific user-role assignment
     */
    int delete(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * Delete all role assignments for a user
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete all user assignments for a role
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}
