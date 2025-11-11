package x.y.z.backend.controller;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Permit;
import x.y.z.backend.service.PermitService;

/**
 * REST Controller for Permit operations.
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
@RequestMapping("/api/permits")
public class PermitController {

    private final PermitService permitService;

    public PermitController(PermitService permitService) {
        this.permitService = permitService;
    }

    /**
     * Get permits for the current user with pagination.
     * GET /api/permits/my?page=0&size=10
     */
    @GetMapping("/my")
    public ResponseEntity<PageResponse<Permit>> getMyPermits(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size) {
        
        // Extract current user from security context
        String currentUser = getCurrentUsername();
        
        // Delegate to service
        PageResponse<Permit> permitPage = permitService.getPermitsByUser(currentUser, page, size);
        
        return ResponseEntity.ok(permitPage);
    }

    /**
     * Get permit by ID.
     * GET /api/permits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Permit> getPermitById(@PathVariable @Min(1) Long id) {
        // Delegate to service
        Permit permit = permitService.getPermitById(id);
        
        return ResponseEntity.ok(permit);
    }

    /**
     * Get permit by permit number.
     * GET /api/permits/number/{permitNumber}
     */
    @GetMapping("/number/{permitNumber}")
    public ResponseEntity<Permit> getPermitByNumber(@PathVariable String permitNumber) {
        // Delegate to service
        Permit permit = permitService.getPermitByNumber(permitNumber);
        
        return ResponseEntity.ok(permit);
    }

    /**
     * Extract current username from Spring Security context.
     * Returns "anonymous" if not authenticated (for testing with security disabled).
     */
    private String getCurrentUsername() {
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
    }
}
