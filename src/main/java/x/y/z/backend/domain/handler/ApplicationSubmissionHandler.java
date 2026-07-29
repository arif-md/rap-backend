package x.y.z.backend.domain.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import x.y.z.backend.controller.dto.ApplicationSubmissionRequest;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.domain.model.University;
import x.y.z.backend.repository.mapper.ApplicationMapper;
import x.y.z.backend.repository.mapper.UniversityMapper;
import x.y.z.backend.repository.mapper.WorkflowAppAssocMapper;

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
    private final UniversityMapper universityMapper;
    private final WorkflowAppAssocMapper workflowAppAssocMapper;

    public ApplicationSubmissionHandler(ApplicationMapper applicationMapper, UniversityMapper universityMapper,
            WorkflowAppAssocMapper workflowAppAssocMapper) {
        this.applicationMapper = applicationMapper;
        this.universityMapper = universityMapper;
        this.workflowAppAssocMapper = workflowAppAssocMapper;
    }

    /**
     * Find the process instance already associated with an application via
     * RAP.WORKFLOW_APP_ASSOC (written by the processes module's ProcessEventListener
     * when a process starts), or null if none has been started yet.
     */
    public Long findActiveProcessInstanceId(Long applicationId) {
        return workflowAppAssocMapper.findActiveProcessInstanceId(applicationId);
    }

    /**
     * Reverse lookup: the application id associated with a jBPM process instance,
     * or null if this process instance wasn't started through the submission flow.
     */
    public Long findApplicationIdByProcessInstanceId(Long processInstanceId) {
        return workflowAppAssocMapper.findApplicationIdByProcessInstanceId(processInstanceId);
    }

    /**
     * Create or update an Application from the submission/save request.
     * A null {@code request.getApplicationId()} creates a new application; a
     * non-null id updates the existing application in place.
     *
     * @param request the application submission request
     * @param username the authenticated username
     * @return the created or updated Application, reflecting current DB state
     *         (including any previously started process instance id)
     */
    public Application saveApplicationFromRequest(ApplicationSubmissionRequest request, String username) {
        if (request.getApplicationId() != null) {
            return updateApplicationFromRequest(request, username);
        }
        return createApplicationFromRequest(request, username);
    }

    private Application createApplicationFromRequest(ApplicationSubmissionRequest request, String username) {
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

        // Look up university by name and set university_id
        resolveUniversity(application, request.getUniversity());

        // Insert into database using MyBatis
        int rowsInserted = applicationMapper.insert(application);

        if (rowsInserted == 0) {
            throw new RuntimeException("Failed to insert application into database");
        }

        logger.info("Application inserted successfully with code: {}", application.getApplicationCode());

        return application;
    }

    private Application updateApplicationFromRequest(ApplicationSubmissionRequest request, String username) {
        logger.info("Updating application {} from request for user: {}", request.getApplicationId(), username);

        Application application = applicationMapper.findById(request.getApplicationId());
        if (application == null) {
            throw new IllegalArgumentException("Application not found for id: " + request.getApplicationId());
        }

        application.setApplicationName(request.getApplicationName());
        application.setDescription(buildDescription(request));
        application.setOwnerName(request.getFirstName() + " " + request.getLastName());
        application.setOwnerEmail(request.getEmail());
        application.setUpdatedBy(username);

        resolveUniversity(application, request.getUniversity());

        int rowsUpdated = applicationMapper.updateSubmission(application);
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update application in database");
        }

        logger.info("Application updated successfully with code: {}", application.getApplicationCode());

        return application;
    }

    private void resolveUniversity(Application application, String universityName) {
        if (universityName != null && !universityName.isEmpty()) {
            University university = universityMapper.findByName(universityName);
            if (university != null) {
                application.setUniversityId(university.getId());
                logger.info("Resolved university '{}' to ID: {}", universityName, university.getId());
            } else {
                logger.warn("University not found by name: '{}'", universityName);
            }
        }
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
