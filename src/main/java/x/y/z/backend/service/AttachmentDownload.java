package x.y.z.backend.service;

import org.springframework.core.io.Resource;

import x.y.z.backend.domain.model.Attachment;

/**
 * Transport object between AttachmentService and AttachmentController: the resolved file
 * bytes plus the metadata row the controller needs to set response headers.
 */
public class AttachmentDownload {

    private final Resource resource;
    private final Attachment attachment;

    public AttachmentDownload(Resource resource, Attachment attachment) {
        this.resource = resource;
        this.attachment = attachment;
    }

    public Resource getResource() {
        return resource;
    }

    public Attachment getAttachment() {
        return attachment;
    }
}
