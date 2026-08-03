package x.y.z.backend.handler;

import java.util.List;

import org.springframework.stereotype.Component;

import x.y.z.backend.domain.model.Attachment;
import x.y.z.backend.repository.mapper.AttachmentMapper;

/**
 * AttachmentHandler - Handles data access for attachments. Pure data access - no business
 * logic (ownership/role checks live in AttachmentService).
 */
@Component
public class AttachmentHandler {

    private final AttachmentMapper attachmentMapper;

    public AttachmentHandler(AttachmentMapper attachmentMapper) {
        this.attachmentMapper = attachmentMapper;
    }

    public List<Attachment> findByApplicationId(Long applicationId) {
        return attachmentMapper.findByApplicationId(applicationId);
    }

    public Attachment findById(Long id) {
        return attachmentMapper.findById(id);
    }
}
