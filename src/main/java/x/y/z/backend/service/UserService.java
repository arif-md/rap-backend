package x.y.z.backend.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.model.Role;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.handler.UserHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UserService - Service layer for user management and OIDC integration.
 * 
 * Responsibilities:
 * - Create/update users from OIDC authentication
 * - Manage user roles
 * - Handle user activation/deactivation
 */
@Service
@Transactional
public class UserService {

    private final UserHandler userHandler;

    public UserService(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    /**
     * Get or create user from OIDC authentication
     * 
     * @param oidcUser OIDC user information from authentication provider
     * @return User entity (existing or newly created)
     */
    public User getOrCreateUserFromOidc(OidcUser oidcUser) {
        String oidcSubject = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        
        // oidcUser.getFullName() may be null if the OIDC provider doesn't return it
        // Use email as fallback if fullName is not available
        String fullName = oidcUser.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = email; // Fallback to email if no name provided
        }

        // Check if user already exists
        User existingUser = userHandler.findByOidcSubject(oidcSubject);
        
        if (existingUser != null) {
            // Update last login time
            userHandler.updateLastLogin(existingUser.getId(), LocalDateTime.now());
            
            // Update user info if changed (with null-safe comparison)
            boolean emailChanged = email != null && !email.equals(existingUser.getEmail());
            boolean nameChanged = fullName != null && !fullName.equals(existingUser.getFullName());
            
            if (emailChanged || nameChanged) {
                if (email != null) {
                    existingUser.setEmail(email);
                }
                if (fullName != null) {
                    existingUser.setFullName(fullName);
                }
                userHandler.update(existingUser);
            }
            
            return existingUser;
        }

        // Create new user
        User newUser = new User(oidcSubject, email, fullName);
        newUser.setLastLoginAt(LocalDateTime.now());
        userHandler.insert(newUser);

        // Assign default "USER" role
        assignDefaultRole(newUser.getId());

        return newUser;
    }

    /**
     * Assign default "USER" role to new user
     * 
     * @param userId User's unique ID
     */
    private void assignDefaultRole(UUID userId) {
        Role userRole = userHandler.findRoleByName("USER");
        
        if (userRole != null) {
            userHandler.assignRole(userId, userRole.getId(), "SYSTEM");
        } else {
            System.err.println("Warning: Default 'USER' role not found in database");
        }
    }

    /**
     * Find user by ID
     * 
     * @param userId User's unique ID
     * @return User entity or null
     */
    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userHandler.findById(userId);
    }

    /**
     * Find user by OIDC subject
     * 
     * @param oidcSubject OIDC subject (sub claim)
     * @return User entity or null
     */
    @Transactional(readOnly = true)
    public User findByOidcSubject(String oidcSubject) {
        return userHandler.findByOidcSubject(oidcSubject);
    }

    /**
     * Find user by email
     * 
     * @param email User's email
     * @return User entity or null
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userHandler.findByEmail(email);
    }

    /**
     * Assign role to user
     * 
     * @param userId User's unique ID
     * @param roleName Role name (e.g., "ADMIN", "MANAGER")
     * @param grantedBy Who granted the role (for audit)
     */
    public void assignRole(UUID userId, String roleName, String grantedBy) {
        Role role = userHandler.findRoleByName(roleName);
        
        if (role == null) {
            throw new IllegalArgumentException("Role '" + roleName + "' not found");
        }

        if (userHandler.hasRole(userId, role.getId())) {
            throw new IllegalArgumentException("User already has role '" + roleName + "'");
        }

        userHandler.assignRole(userId, role.getId(), grantedBy);
    }

    /**
     * Remove role from user
     * 
     * @param userId User's unique ID
     * @param roleName Role name
     */
    public void removeRole(UUID userId, String roleName) {
        Role role = userHandler.findRoleByName(roleName);
        
        if (role == null) {
            throw new IllegalArgumentException("Role '" + roleName + "' not found");
        }

        userHandler.removeRole(userId, role.getId());
    }

    /**
     * Deactivate user (soft delete)
     * 
     * @param userId User's unique ID
     */
    public void deactivateUser(UUID userId) {
        userHandler.deactivate(userId);
    }

    /**
     * Activate user
     * 
     * @param userId User's unique ID
     */
    public void activateUser(UUID userId) {
        userHandler.activate(userId);
    }

    /**
     * Get all active users
     * 
     * @return List of active users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userHandler.findAllActive();
    }

    /**
     * Get all users (including inactive)
     * 
     * @return List of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userHandler.findAll();
    }
}
