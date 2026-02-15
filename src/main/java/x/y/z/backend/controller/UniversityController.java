package x.y.z.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import x.y.z.backend.domain.model.University;
import x.y.z.backend.service.UniversityService;

import java.util.List;

/**
 * REST controller for University reference data.
 */
@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    private final UniversityService universityService;

    public UniversityController(UniversityService universityService) {
        this.universityService = universityService;
    }

    /**
     * Get all active universities (for picklist).
     */
    @GetMapping
    @PreAuthorize("hasRole('INTERNAL_USER')")
    public ResponseEntity<List<University>> getActiveUniversities() {
        List<University> universities = universityService.getActiveUniversities();
        return ResponseEntity.ok(universities);
    }

    /**
     * Get university by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INTERNAL_USER')")
    public ResponseEntity<University> getUniversityById(@PathVariable Long id) {
        University university = universityService.getUniversityById(id);
        return ResponseEntity.ok(university);
    }
}
