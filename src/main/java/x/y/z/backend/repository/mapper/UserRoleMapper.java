package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

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
    UserRole findById(@Param("id") UUID id);

    /**
     * Get all role assignments for a specific user
     */
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    /**
     * Get all user assignments for a specific role
     */
    List<UserRole> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * Check if a user has a specific role
     */
    boolean hasRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * Delete a specific user-role assignment
     */
    int delete(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * Delete all role assignments for a user
     */
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Delete all user assignments for a role
     */
    int deleteByRoleId(@Param("roleId") UUID roleId);
}
