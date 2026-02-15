package x.y.z.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import x.y.z.backend.domain.model.University;
import x.y.z.backend.exception.ResourceNotFoundException;
import x.y.z.backend.handler.UniversityHandler;

import java.util.List;

/**
 * UniversityService - Service layer for University entities.
 * Read-only reference data.
 */
@Service
@Transactional(readOnly = true)
public class UniversityService {

    private final UniversityHandler universityHandler;

    public UniversityService(UniversityHandler universityHandler) {
        this.universityHandler = universityHandler;
    }

    /**
     * Get all active universities.
     */
    public List<University> getActiveUniversities() {
        return universityHandler.findByStatus("ACTIVE");
    }

    /**
     * Get all universities regardless of status.
     */
    public List<University> getAllUniversities() {
        return universityHandler.findAll();
    }

    /**
     * Get university by ID.
     */
    public University getUniversityById(Long id) {
        University university = universityHandler.findById(id);
        if (university == null) {
            throw new ResourceNotFoundException("University", id);
        }
        return university;
    }

    /**
     * Get university by code.
     */
    public University getUniversityByCode(String code) {
        University university = universityHandler.findByCode(code);
        if (university == null) {
            throw new ResourceNotFoundException("University", code);
        }
        return university;
    }
}
