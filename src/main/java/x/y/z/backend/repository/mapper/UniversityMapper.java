package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import x.y.z.backend.domain.model.University;

import java.util.List;

/**
 * MyBatis Mapper interface for University entities.
 */
@Mapper
@Repository
public interface UniversityMapper {

    List<University> findAll();

    List<University> findByStatus(@Param("status") String status);

    University findById(@Param("id") Long id);

    University findByCode(@Param("universityCode") String universityCode);

    long count();
}
