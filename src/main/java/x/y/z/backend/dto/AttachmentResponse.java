package x.y.z.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for attachment list responses. Deliberately excludes storageLocation - that's an
 * internal detail resolved server-side by AttachmentStorageService, never exposed to clients.
 */
public class AttachmentResponse {

    private Long id;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private LocalDateTime createdAt;

    public AttachmentResponse() {
    }

    public AttachmentResponse(Long id, String fileName, String contentType, Long fileSizeBytes, LocalDateTime createdAt) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
