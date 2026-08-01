package x.y.z.backend.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import x.y.z.backend.domain.model.ApplicationInternalReview;

/**
 * MyBatis Mapper interface for ApplicationInternalReview entity.
 * Queries are defined in ApplicationInternalReviewMapper.xml
 */
@Mapper
@Repository
public interface ApplicationInternalReviewMapper {

    int insert(ApplicationInternalReview review);
}
