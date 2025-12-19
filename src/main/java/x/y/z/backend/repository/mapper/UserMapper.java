package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis Mapper interface for User entity.
 * Queries are defined in UserMapper.xml
 */
@Mapper
@Repository
public interface UserMapper {

    /**
     * Insert a new user record
     */
    int insert(User user);

    /**
     * Update an existing user record
     */
    int update(User user);

    /**
     * Find user by ID
     */
    User findById(@Param("id") Long id);

    /**
     * Find user by OIDC subject (sub claim)
     */
    User findByOidcSubject(@Param("oidcSubject") String oidcSubject);

    /**
     * Find user by email
     */
    User findByEmail(@Param("email") String email);

    /**
     * Get all users
     */
    List<User> findAll();

    /**
     * Get all active users
     */
    List<User> findAllActive();

    /**
     * Update user's last login timestamp
     */
    int updateLastLogin(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * Deactivate a user (soft delete)
     */
    int deactivate(@Param("id") Long id);

    /**
     * Activate a user
     */
    int activate(@Param("id") Long id);

    /**
     * Delete a user by ID (hard delete - use with caution)
     */
    int deleteById(@Param("id") Long id);
}
