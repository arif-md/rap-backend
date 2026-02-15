package x.y.z.backend.handler;

import org.springframework.stereotype.Component;
import x.y.z.backend.domain.model.University;
import x.y.z.backend.repository.mapper.UniversityMapper;

import java.util.List;

/**
 * UniversityHandler - Data access component for University entities.
 * Wraps MyBatis mapper calls.
 */
@Component
public class UniversityHandler {

    private final UniversityMapper universityMapper;

    public UniversityHandler(UniversityMapper universityMapper) {
        this.universityMapper = universityMapper;
    }

    public List<University> findAll() {
        return universityMapper.findAll();
    }

    public List<University> findByStatus(String status) {
        return universityMapper.findByStatus(status);
    }

    public University findById(Long id) {
        return universityMapper.findById(id);
    }

    public University findByCode(String universityCode) {
        return universityMapper.findByCode(universityCode);
    }

    public long count() {
        return universityMapper.count();
    }
}
