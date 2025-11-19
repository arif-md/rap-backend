package x.y.z.backend.controller;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import x.y.z.backend.config.CurrentUser;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Task;
import x.y.z.backend.service.ProcessService;

/**
 * REST Controller for Workflow Task operations.
 * 
 * Responsibilities:
 * - HTTP request/response handling
 * - Request validation
 * - Extracting authenticated user from security context
 * - Returning appropriate HTTP status codes
 * 
 * Does NOT contain:
 * - Business logic (in Service layer)
 * - Data access (in Handler layer)
 * - Exception handling (delegated to GlobalExceptionHandler)
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private final ProcessService processService;

    public WorkflowController(ProcessService processService) {
        this.processService = processService;
    }

    /**
     * Get tasks assigned to the current user with pagination.
     * GET /api/workflow/tasks?page=0&size=10
     */
    @GetMapping("/tasks")
    public ResponseEntity<PageResponse<Task>> getMyTasks(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
            CurrentUser user) {
        
        // Extract current user from security context
        String currentUser = user.getEmail();
        
        // Delegate to service
        PageResponse<Task> taskPage = processService.getTasksByUser(currentUser, page, size);
        
        return ResponseEntity.ok(taskPage);
    }

    /**
     * Get task by ID.
     * GET /api/workflow/tasks/{id}
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable @Min(1) Long id) {
        // Delegate to service
        Task task = processService.getTaskById(id);
        
        return ResponseEntity.ok(task);
    }

    /**
     * Extract current authenticated username from Security Context.
     * Returns "anonymous" if not authenticated (for testing with security disabled).
     */
    /*private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            
            // Handle UserPrincipal from JWT authentication
            if (principal instanceof x.y.z.backend.security.JwtAuthenticationFilter.UserPrincipal) {
                return ((x.y.z.backend.security.JwtAuthenticationFilter.UserPrincipal) principal).getEmail();
            }
            
            // Fallback to getName() for other authentication types (OIDC, etc.)
            return authentication.getName();
        }
        
        return "anonymous";
    }*/
}
