package x.y.z.backend.controller.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for completing an internal review task with a captured signature + review date.
 */
public class CompleteTaskWithReviewRequest {

    @NotNull(message = "applicationId is required")
    private Long applicationId;

    @NotNull(message = "reviewDate is required")
    private LocalDate reviewDate;

    @NotBlank(message = "signatureImage is required")
    private String signatureImage;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getSignatureImage() {
        return signatureImage;
    }

    public void setSignatureImage(String signatureImage) {
        this.signatureImage = signatureImage;
    }
}
