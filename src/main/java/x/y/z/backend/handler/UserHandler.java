package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.Role;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.domain.model.UserRole;
import x.y.z.backend.repository.mapper.RoleMapper;
import x.y.z.backend.repository.mapper.UserMapper;
import x.y.z.backend.repository.mapper.UserRoleMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UserHandler - Handles user-related data access operations.
 * Pure data access - business logic should be in the Service layer.
 */
@Component
public class UserHandler {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserHandler(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public User insert(User user) {
        int rowsInserted = userMapper.insert(user);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert user");
        }
        return user;
    }

    public User update(User user) {
        int rowsUpdated = userMapper.update(user);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update user");
        }
        return user;
    }

    public User findById(UUID id) {
        return userMapper.findById(id);
    }

    public User findByOidcSubject(String oidcSubject) {
        return userMapper.findByOidcSubject(oidcSubject);
    }

    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public List<User> findAllActive() {
        return userMapper.findAllActive();
    }

    public void updateLastLogin(UUID userId, LocalDateTime lastLoginAt) {
        userMapper.updateLastLogin(userId, lastLoginAt);
    }

    public void deactivate(UUID userId) {
        userMapper.deactivate(userId);
    }

    public void activate(UUID userId) {
        userMapper.activate(userId);
    }

    public void deleteById(UUID userId) {
        userMapper.deleteById(userId);
    }

    // Role-related operations
    public Role findRoleByName(String roleName) {
        return roleMapper.findByName(roleName);
    }

    public List<Role> findRolesByUserId(UUID userId) {
        return roleMapper.findByUserId(userId);
    }

    public void assignRole(UUID userId, UUID roleId, String grantedBy) {
        UserRole userRole = new UserRole(userId, roleId, grantedBy);
        userRoleMapper.insert(userRole);
    }

    public boolean hasRole(UUID userId, UUID roleId) {
        return userRoleMapper.hasRole(userId, roleId);
    }

    public void removeRole(UUID userId, UUID roleId) {
        userRoleMapper.delete(userId, roleId);
    }

    public boolean existsByOidcSubject(String oidcSubject) {
        return userMapper.findByOidcSubject(oidcSubject) != null;
    }

    public boolean existsByEmail(String email) {
        return userMapper.findByEmail(email) != null;
    }
}
