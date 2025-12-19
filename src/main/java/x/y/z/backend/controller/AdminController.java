package x.y.z.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.domain.model.Role;
import x.y.z.backend.domain.model.User;
import x.y.z.backend.handler.UserHandler;
import x.y.z.backend.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController - REST Controller for admin operations.
 * 
 * IMPORTANT: All endpoints require ADMIN role.
 * 
 * Endpoints:
 * - GET /api/admin/users - Get all users
 * - GET /api/admin/users/active - Get all active users with their session info
 * - POST /api/admin/users/{userId}/roles/{roleName} - Assign role to user
 * - DELETE /api/admin/users/{userId}/roles/{roleName} - Remove role from user
 * - PUT /api/admin/users/{userId}/deactivate - Deactivate user
 * - PUT /api/admin/users/{userId}/activate - Activate user
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final UserHandler userHandler;

    public AdminController(UserService userService, UserHandler userHandler) {
        this.userService = userService;
        this.userHandler = userHandler;
    }

    /**
     * GET /api/admin/users
     * Get all users (including inactive)
     * 
     * @return List of all users
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            
            List<Map<String, Object>> userList = users.stream()
                    .map(this::mapUserToResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", userList);
            response.put("total", userList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * GET /api/admin/users/active
     * Get all active users with their roles and session information.
     * 
     * NOTE: This endpoint shows users with active accounts (is_active=true).
     * To see users with currently valid access tokens, you would need to:
     * 1. Query revoked_tokens table for non-revoked JTIs
     * 2. Parse JWT tokens to extract user IDs
     * 3. Cross-reference with users table
     * 
     * For now, this returns users who are not deactivated by admin.
     * In the future, you can enhance this to show real-time active sessions
     * by tracking access tokens or implementing a session management table.
     * 
     * @return List of active users with their roles
     */
    @GetMapping("/users/active")
    public ResponseEntity<Map<String, Object>> getActiveUsers() {
        try {
            List<User> activeUsers = userService.getAllActiveUsers();
            
            List<Map<String, Object>> userList = activeUsers.stream()
                    .map(user -> {
                        Map<String, Object> userMap = mapUserToResponse(user);
                        
                        // Add roles for each user
                        List<Role> roles = userHandler.findRolesByUserId(user.getId());
                        List<String> roleNames = roles.stream()
                                .map(Role::getRoleName)
                                .collect(Collectors.toList());
                        userMap.put("roles", roleNames);
                        
                        return userMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("users", userList);
            response.put("total", userList.size());
            response.put("note", "Shows users with active accounts. For real-time session tracking, consider implementing session management or Redis cache.");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get active users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/admin/users/{userId}/roles/{roleName}
     * Assign role to user
     * 
     * @param userId User's unique ID
     * @param roleName Role name to assign (e.g., "ADMIN", "MANAGER")
     * @param adminEmail Email of admin performing the action (from request body)
     * @return Success response
     */
    @PostMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable Long userId,
            @PathVariable String roleName,
            @RequestBody Map<String, String> requestBody) {

        try {
            String grantedBy = requestBody.getOrDefault("grantedBy", "ADMIN");
            
            userService.assignRole(userId, roleName, grantedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role '" + roleName + "' assigned to user " + userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to assign role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * DELETE /api/admin/users/{userId}/roles/{roleName}
     * Remove role from user
     * 
     * @param userId User's unique ID
     * @param roleName Role name to remove
     * @return Success response
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Map<String, Object>> removeRole(
            @PathVariable Long userId,
            @PathVariable String roleName) {

        try {
            userService.removeRole(userId, roleName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Role '" + roleName + "' removed from user " + userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to remove role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * PUT /api/admin/users/{userId}/deactivate
     * Deactivate user (soft delete)
     * 
     * @param userId User's unique ID
     * @return Success response
     */
    @PutMapping("/users/{userId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long userId) {
        try {
            userService.deactivateUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User " + userId + " deactivated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to deactivate user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * PUT /api/admin/users/{userId}/activate
     * Activate user
     * 
     * @param userId User's unique ID
     * @return Success response
     */
    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        try {
            userService.activateUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User " + userId + " activated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to activate user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Helper: Map User entity to response DTO
     */
    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId().toString());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getFullName());
        userMap.put("oidcSubject", user.getOidcSubject());
        userMap.put("isActive", user.getIsActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        return userMap;
    }
}
