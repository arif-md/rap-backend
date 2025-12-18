package x.y.z.backend.domain.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import x.y.z.backend.controller.dto.ApplicationSubmissionRequest;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.repository.mapper.ApplicationMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handler for application submission data operations.
 * Responsible for mapping DTOs to domain models and database interactions.
 */
@Component
public class ApplicationSubmissionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSubmissionHandler.class);

    private final ApplicationMapper applicationMapper;

    public ApplicationSubmissionHandler(ApplicationMapper applicationMapper) {
        this.applicationMapper = applicationMapper;
    }

    /**
     * Create and persist an Application from the submission request.
     * 
     * @param request the application submission request
     * @param username the authenticated username
     * @return the created Application with generated ID and code
     */
    public Application createApplicationFromRequest(ApplicationSubmissionRequest request, String username) {
        logger.info("Creating application from request for user: {}", username);

        // Create Application entity
        Application application = new Application();
        
        // Map fields from request
        application.setApplicationName(request.getApplicationName());
        application.setApplicationCode(generateApplicationCode());
        application.setDescription(buildDescription(request));
        application.setStatus("PENDING");
        application.setOwnerName(request.getFirstName() + " " + request.getLastName());
        application.setOwnerEmail(request.getEmail());
        application.setCreatedBy(username);
        application.setUpdatedBy(username);

        // Insert into database using MyBatis
        int rowsInserted = applicationMapper.insert(application);

        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert application into database");
        }

        logger.info("Application inserted successfully with code: {}", application.getApplicationCode());
        
        return application;
    }

    /**
     * Generate a unique application code.
     * Format: APP-YYYYMMDD-HHMMSS-XXX
     */
    private String generateApplicationCode() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int random = (int) (Math.random() * 1000);
        return String.format("APP-%s-%03d", timestamp, random);
    }

    /**
     * Build description from request fields.
     */
    private String buildDescription(ApplicationSubmissionRequest request) {
        StringBuilder desc = new StringBuilder();
        desc.append("University: ").append(request.getUniversity()).append("\n");
        desc.append("Program: ").append(request.getProgram()).append("\n");
        desc.append("Phone: ").append(request.getPhone()).append("\n");
        
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            desc.append("Additional Info: ").append(request.getDescription());
        }
        
        return desc.toString();
    }
}
