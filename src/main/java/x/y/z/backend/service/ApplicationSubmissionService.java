package x.y.z.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import x.y.z.backend.controller.dto.ApplicationSubmissionRequest;
import x.y.z.backend.domain.handler.ApplicationSubmissionHandler;
import x.y.z.backend.domain.model.Application;

/**
 * Service layer for application submission business logic.
 * Handles transactions and delegates to handler for data operations.
 */
@Service
public class ApplicationSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSubmissionService.class);

    private final ApplicationSubmissionHandler applicationSubmissionHandler;

    public ApplicationSubmissionService(ApplicationSubmissionHandler applicationSubmissionHandler) {
        this.applicationSubmissionHandler = applicationSubmissionHandler;
    }

    /**
     * Submit a new university admission application.
     * 
     * @param request the application submission request
     * @param username the authenticated username
     * @return the created Application entity
     */
    @Transactional
    public Application submitApplication(ApplicationSubmissionRequest request, String username) {
        logger.info("Submitting application for user: {}", username);

        // Server-side validation
        validateApplicationRequest(request);

        // Delegate to handler for processing
        Application application = applicationSubmissionHandler.createApplicationFromRequest(request, username);

        logger.info("Application created with code: {}", application.getApplicationCode());
        return application;
    }

    /**
     * Perform server-side validation on the application request.
     * 
     * @param request the application submission request
     * @throws IllegalArgumentException if validation fails
     */
    private void validateApplicationRequest(ApplicationSubmissionRequest request) {
        // Additional business validations beyond bean validation
        
        if (!StringUtils.hasText(request.getEmail()) || !isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (!StringUtils.hasText(request.getPhone()) || !isValidPhone(request.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Validate university selection
        if (!isValidUniversity(request.getUniversity())) {
            throw new IllegalArgumentException("Invalid university selection");
        }

        logger.debug("Application request validation passed");
    }

    /**
     * Validate email format (basic check, bean validation handles detailed format).
     */
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    /**
     * Validate phone number format (allows digits, spaces, hyphens, parentheses).
     */
    private boolean isValidPhone(String phone) {
        return phone.matches("[\\d\\s\\-()]+");
    }

    /**
     * Validate university selection against allowed list.
     */
    private boolean isValidUniversity(String university) {
        // Validate against predefined list of universities
        String[] validUniversities = {
            "Harvard University",
            "Stanford University",
            "MIT",
            "Oxford University",
            "Cambridge University",
            "Yale University",
            "Princeton University",
            "Columbia University",
            "University of Chicago",
            "Imperial College London"
        };

        for (String validUniv : validUniversities) {
            if (validUniv.equals(university)) {
                return true;
            }
        }
        return false;
    }
}
