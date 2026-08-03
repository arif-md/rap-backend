package x.y.z.backend.controller;

import java.io.IOException;
import java.util.List;

import jakarta.validation.constraints.Min;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import x.y.z.backend.config.CurrentUser;
import x.y.z.backend.domain.model.Attachment;
import x.y.z.backend.dto.AttachmentResponse;
import x.y.z.backend.security.JwtAuthenticationFilter;
import x.y.z.backend.service.AttachmentDownload;
import x.y.z.backend.service.AttachmentService;

/**
 * REST Controller for listing and downloading an application's attachments (dashboard
 * "Attachments" action, shared by the Applications and Admissions tabs - an admission is just
 * an application whose status is ACCEPTED, see ApplicationController).
 */
@RestController
@RequestMapping("/api/applications")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/{applicationId}/attachments")
    @PreAuthorize("hasRole('EXTERNAL_USER') or hasRole('INTERNAL_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable @Min(1) Long applicationId, CurrentUser user) {
        List<AttachmentResponse> attachments = attachmentService.listByApplication(applicationId, user.getEmail(), isInternalOrAdmin());
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{applicationId}/attachments/{attachmentId}/download")
    @PreAuthorize("hasRole('EXTERNAL_USER') or hasRole('INTERNAL_USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable @Min(1) Long applicationId,
            @PathVariable @Min(1) Long attachmentId, CurrentUser user) throws IOException {
        AttachmentDownload download = attachmentService.downloadAttachment(applicationId, attachmentId, user.getEmail(), isInternalOrAdmin());
        Attachment attachment = download.getAttachment();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(download.getResource());
    }

    /**
     * Internal/admin staff aren't the application owner, so the owner-email check in
     * AttachmentService needs to know to bypass it for them - same helper/reasoning as
     * ApplicationController.isInternalOrAdmin().
     */
    private boolean isInternalOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticationFilter.UserPrincipal) {
            List<String> roles = ((JwtAuthenticationFilter.UserPrincipal) authentication.getPrincipal()).getRoles();
            return roles != null && (roles.contains("INTERNAL_USER") || roles.contains("ADMIN"));
        }
        return false;
    }
}
