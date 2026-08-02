package x.y.z.backend.controller;

import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.config.CurrentUser;
import x.y.z.backend.domain.dto.PageResponse;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.domain.model.Permit;
import x.y.z.backend.dto.ApplicationResponse;
import x.y.z.backend.mapper.ApplicationDtoMapper;
import x.y.z.backend.service.ApplicationService;
import x.y.z.backend.service.PermitService;

import java.util.List;
import java.util.stream.Collectors;

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
    private final ApplicationService applicationService;
    private final ApplicationDtoMapper dtoMapper;

    public PermitController(PermitService permitService, ApplicationService applicationService,
            ApplicationDtoMapper dtoMapper) {
        this.permitService = permitService;
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * Get permits for the current user with pagination.
     * An application becomes a permit once its latest workflow status is ACCEPTED,
     * so this is backed by RAP.APPLICATION rather than RAP.permit - see ApplicationService.
     * GET /api/permits/my?page=0&size=10
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('EXTERNAL_USER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> getMyPermits(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size,
            CurrentUser user) {

        PageResponse<Application> applicationPage =
            applicationService.getAcceptedApplicationsByUser(user.getEmail(), page, size);

        List<ApplicationResponse> content = applicationPage.getContent().stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());

        PageResponse<ApplicationResponse> response = new PageResponse<>(
            content,
            applicationPage.getPage(),
            applicationPage.getSize(),
            applicationPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
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
     * Get permits for a specific university with pagination (internal "Permits" tab).
     * An application becomes a permit once its latest workflow status is ACCEPTED,
     * so this is backed by RAP.APPLICATION rather than RAP.permit - mirrors getMyPermits above.
     * GET /api/permits/university/{universityId}?page=0&size=10
     */
    @GetMapping("/university/{universityId}")
    @PreAuthorize("hasRole('INTERNAL_USER')")
    public ResponseEntity<PageResponse<ApplicationResponse>> getPermitsByUniversity(
            @PathVariable Long universityId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size) {
        PageResponse<Application> applicationPage =
            applicationService.getAcceptedApplicationsByUniversity(universityId, page, size);

        List<ApplicationResponse> content = applicationPage.getContent().stream()
            .map(dtoMapper::toDto)
            .collect(Collectors.toList());

        PageResponse<ApplicationResponse> response = new PageResponse<>(
            content,
            applicationPage.getPage(),
            applicationPage.getSize(),
            applicationPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }
}
