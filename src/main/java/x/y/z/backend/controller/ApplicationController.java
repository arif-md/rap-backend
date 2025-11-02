package x.y.z.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.dto.ApplicationResponse;
import x.y.z.backend.dto.CreateApplicationRequest;
import x.y.z.backend.dto.UpdateApplicationRequest;
import x.y.z.backend.mapper.ApplicationDtoMapper;
import x.y.z.backend.service.ApplicationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Application CRUD operations.
 * 
 * Responsibilities:
 * - HTTP request/response handling
 * - Request validation via @Valid
 * - DTO to domain model conversion
 * - Extracting authenticated user from security context
 * - Returning appropriate HTTP status codes
 * 
 * Does NOT contain:
 * - Business logic (in Service layer)
 * - Data access (in Handler layer)
 * - Exception handling (delegated to GlobalExceptionHandler)
 */
@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationDtoMapper dtoMapper;

    public ApplicationController(ApplicationService applicationService, ApplicationDtoMapper dtoMapper) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Create a new application.
     * POST /api/applications
     */
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        
        // Extract current user from security context
        String currentUser = getCurrentUsername();
        
        // Convert DTO to domain model
        Application application = dtoMapper.toEntity(request, currentUser);
        
        // Delegate to service (business logic + transaction)
        Application created = applicationService.createApplication(application);
        
        // Convert domain model to DTO
        ApplicationResponse response = dtoMapper.toDto(created);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing application.
     * PUT /api/applications/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        
        // Extract current user from security context
        String currentUser = getCurrentUsername();
        
        // Convert DTO to domain model
        Application application = dtoMapper.toEntity(id, request, currentUser);
        
        // Delegate to service (business logic + transaction)
        Application updated = applicationService.updateApplication(application);
        
        // Convert domain model to DTO
        ApplicationResponse response = dtoMapper.toDto(updated);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an application.
     * DELETE /api/applications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable @Min(1) Long id) {
        // Delegate to service (business logic + transaction)
        applicationService.deleteApplication(id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Get application by ID.
     * GET /api/applications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable @Min(1) Long id) {
        // Delegate to service
        Application application = applicationService.getApplicationById(id);
        
        // Convert domain model to DTO
        ApplicationResponse response = dtoMapper.toDto(application);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get application by code.
     * GET /api/applications/code/{applicationCode}
     */
    @GetMapping("/code/{applicationCode}")
    public ResponseEntity<ApplicationResponse> getApplicationByCode(@PathVariable String applicationCode) {
        // Delegate to service
        Application application = applicationService.getApplicationByCode(applicationCode);
        
        // Convert domain model to DTO
        ApplicationResponse response = dtoMapper.toDto(application);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all applications.
     * GET /api/applications
     */
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        // Delegate to service
        List<Application> applications = applicationService.getAllApplications();
        
        // Convert list of domain models to list of DTOs
        List<ApplicationResponse> response = applications.stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get applications by status.
     * GET /api/applications/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationResponse>> getApplicationsByStatus(@PathVariable String status) {
        // Delegate to service (includes validation)
        List<Application> applications = applicationService.getApplicationsByStatus(status);
        
        // Convert list of domain models to list of DTOs
        List<ApplicationResponse> response = applications.stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search applications by name.
     * GET /api/applications/search?name={namePattern}
     */
    @GetMapping("/search")
    public ResponseEntity<List<ApplicationResponse>> searchApplicationsByName(
            @RequestParam(name = "name") String namePattern) {
        
        // Delegate to service
        List<Application> applications = applicationService.searchApplicationsByName(namePattern);
        
        // Convert list of domain models to list of DTOs
        List<ApplicationResponse> response = applications.stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get application count.
     * GET /api/applications/count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getApplicationCount() {
        // Delegate to service
        long count = applicationService.getApplicationCount();
        
        // Format response
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extract current username from Spring Security context.
     * Returns "anonymous" if not authenticated (for testing with security disabled).
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        
        return "anonymous";
    }
}
