package x.y.z.backend.controller;

import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import x.y.z.backend.domain.model.Application;
import x.y.z.backend.dto.ApplicationResponse;
import x.y.z.backend.mapper.ApplicationDtoMapper;
import x.y.z.backend.service.ApplicationService;

/**
 * REST Controller backing the dashboard's "actions" menu (e.g. "View Application").
 * Reuses the existing ApplicationService/ApplicationHandler rather than duplicating
 * application data-access logic.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ApplicationService applicationService;
    private final ApplicationDtoMapper dtoMapper;

    public DashboardController(ApplicationService applicationService, ApplicationDtoMapper dtoMapper) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
    }

    /**
     * View application details (read-only) for the dashboard's "View Application" action.
     * GET /api/dashboard/applications/{id}
     */
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationResponse> viewApplication(@PathVariable @Min(1) Long id) {
        Application application = applicationService.getApplicationById(id);
        ApplicationResponse response = dtoMapper.toDto(application);
        return ResponseEntity.ok(response);
    }
}
