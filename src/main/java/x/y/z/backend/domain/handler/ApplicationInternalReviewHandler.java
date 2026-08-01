package x.y.z.backend.domain.handler;

import org.springframework.stereotype.Component;

import x.y.z.backend.domain.model.ApplicationInternalReview;
import x.y.z.backend.repository.mapper.ApplicationInternalReviewMapper;

/**
 * ApplicationInternalReviewHandler - data access for internal reviewer signatures.
 * Pure data access - no business logic.
 */
@Component
public class ApplicationInternalReviewHandler {

    private final ApplicationInternalReviewMapper applicationInternalReviewMapper;

    public ApplicationInternalReviewHandler(ApplicationInternalReviewMapper applicationInternalReviewMapper) {
        this.applicationInternalReviewMapper = applicationInternalReviewMapper;
    }

    public ApplicationInternalReview insert(ApplicationInternalReview review) {
        int rowsInserted = applicationInternalReviewMapper.insert(review);
        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert application internal review");
        }
        return review;
    }
}
