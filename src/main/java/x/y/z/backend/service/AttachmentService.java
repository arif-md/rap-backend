package x.y.z.backend.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import x.y.z.backend.domain.model.Application;
import x.y.z.backend.domain.model.Attachment;
import x.y.z.backend.dto.AttachmentResponse;
import x.y.z.backend.exception.ResourceNotFoundException;
import x.y.z.backend.handler.ApplicationHandler;
import x.y.z.backend.handler.AttachmentHandler;
import x.y.z.backend.storage.AttachmentStorageService;

@Service
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentHandler attachmentHandler;
    private final ApplicationHandler applicationHandler;
    private final AttachmentStorageService attachmentStorageService;

    public AttachmentService(AttachmentHandler attachmentHandler, ApplicationHandler applicationHandler,
            AttachmentStorageService attachmentStorageService) {
        this.attachmentHandler = attachmentHandler;
        this.applicationHandler = applicationHandler;
        this.attachmentStorageService = attachmentStorageService;
    }

    /**
     * List attachments for an application (dashboard "Attachments" dialog, both the
     * Applications and Permits tabs - a permit is just an application, see ApplicationService).
     * BUSINESS LOGIC: same trust model as getProcessStatusImage - the application's owner may
     * always view its attachments; internal/admin staff may view any application's attachments.
     */
    public List<AttachmentResponse> listByApplication(Long applicationId, String requesterEmail, boolean callerIsInternalOrAdmin) {
        Application application = requireAccessibleApplication(applicationId, requesterEmail, callerIsInternalOrAdmin);

        return attachmentHandler.findByApplicationId(application.getId()).stream()
                .map(a -> new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getFileSizeBytes(), a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * Resolve an attachment's bytes for download, after the same ownership/role check as
     * listByApplication, plus a check that the attachment actually belongs to the application
     * in the request path (defense against a mismatched-id IDOR via the attachmentId alone).
     */
    public AttachmentDownload downloadAttachment(Long applicationId, Long attachmentId, String requesterEmail, boolean callerIsInternalOrAdmin) throws IOException {
        Application application = requireAccessibleApplication(applicationId, requesterEmail, callerIsInternalOrAdmin);

        Attachment attachment = attachmentHandler.findById(attachmentId);
        if (attachment == null || !attachment.getApplicationId().equals(application.getId())) {
            throw new ResourceNotFoundException("Attachment", attachmentId);
        }

        return new AttachmentDownload(attachmentStorageService.load(attachment.getStorageLocation()), attachment);
    }

    private Application requireAccessibleApplication(Long applicationId, String requesterEmail, boolean callerIsInternalOrAdmin) {
        Application application = applicationHandler.findById(applicationId);
        if (application == null) {
            throw new ResourceNotFoundException("Application", applicationId);
        }
        boolean isOwner = application.getOwnerEmail() != null && application.getOwnerEmail().equalsIgnoreCase(requesterEmail);
        if (!isOwner && !callerIsInternalOrAdmin) {
            throw new AccessDeniedException("You do not have access to this application's attachments");
        }
        return application;
    }
}
