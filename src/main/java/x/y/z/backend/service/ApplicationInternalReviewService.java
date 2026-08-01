package x.y.z.backend.service;

import java.time.LocalDate;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import x.y.z.backend.domain.handler.ApplicationInternalReviewHandler;
import x.y.z.backend.domain.model.ApplicationInternalReview;

/**
 * Persists the internal reviewer's signature + review date captured on the internal
 * application review page ("My Tasks" -> Complete), ahead of completing the jBPM task.
 */
@Service
public class ApplicationInternalReviewService {

    private final ApplicationInternalReviewHandler applicationInternalReviewHandler;

    public ApplicationInternalReviewService(ApplicationInternalReviewHandler applicationInternalReviewHandler) {
        this.applicationInternalReviewHandler = applicationInternalReviewHandler;
    }

    @Transactional
    public void saveReview(Long applicationId, Long taskId, Long reviewerUserId, LocalDate reviewDate,
            String signatureImageDataUrl) {
        ApplicationInternalReview review = new ApplicationInternalReview();
        review.setApplicationId(applicationId);
        review.setTaskId(taskId);
        review.setReviewerUserId(reviewerUserId);
        review.setReviewDate(reviewDate);
        review.setSignatureImage(decodeSignature(signatureImageDataUrl));
        applicationInternalReviewHandler.insert(review);
    }

    // The frontend sends canvas.toDataURL()'s output ("data:image/png;base64,<payload>") -
    // strip the data-URL prefix before decoding, tolerating a bare base64 string too.
    private byte[] decodeSignature(String signatureImageDataUrl) {
        if (signatureImageDataUrl == null || signatureImageDataUrl.isBlank()) {
            throw new IllegalArgumentException("signatureImage is required");
        }
        String base64 = signatureImageDataUrl;
        int commaIndex = signatureImageDataUrl.indexOf(',');
        if (signatureImageDataUrl.startsWith("data:") && commaIndex >= 0) {
            base64 = signatureImageDataUrl.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(base64);
    }
}
