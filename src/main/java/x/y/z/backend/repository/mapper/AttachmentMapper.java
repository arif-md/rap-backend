package x.y.z.backend.repository.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import x.y.z.backend.domain.model.Attachment;

/**
 * MyBatis Mapper interface for Attachment entity.
 * Queries are defined in AttachmentMapper.xml
 */
@Mapper
@Repository
public interface AttachmentMapper {

    List<Attachment> findByApplicationId(@Param("applicationId") Long applicationId);

    Attachment findById(@Param("id") Long id);
}
