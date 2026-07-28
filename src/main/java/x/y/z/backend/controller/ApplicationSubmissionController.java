package x.y.z.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import x.y.z.backend.controller.dto.ApplicationSubmissionRequest;
import x.y.z.backend.controller.dto.ApplicationSubmissionResponse;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.domain.model.Constants;
import x.y.z.backend.domain.model.ProcessInfo;
import x.y.z.backend.security.JwtAuthenticationFilter;
import x.y.z.backend.service.ApplicationSubmissionService;

/**
 * REST Controller for handling university admission application submissions.
 * Requires authentication for all endpoints.
 */
@RestController
@RequestMapping("/api/applications/submissions")
public class ApplicationSubmissionController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSubmissionController.class);

    private final ApplicationSubmissionService applicationSubmissionService;

    public ApplicationSubmissionController(ApplicationSubmissionService applicationSubmissionService) {
        this.applicationSubmissionService = applicationSubmissionService;
    }

    /**
     * Submit a university admission application. Creates a new application if
     * {@code request.applicationId} is null, otherwise updates the existing one.
     * Always ends with a started jBPM process (started here if not already running).
     *
     * @param request the application submission request with validated data
     * @return ResponseEntity containing the application number and success message
     */
    @PostMapping
    @PreAuthorize("hasRole('EXTERNAL_USER')")
    public ResponseEntity<ApplicationSubmissionResponse> submitApplication(
            @Valid @RequestBody ApplicationSubmissionRequest request) {

        logger.info("Received application submission request: {}", request);

        Application application = processApplicationAndStartWorkflow(request);

        ApplicationSubmissionResponse response = new ApplicationSubmissionResponse(
            application.getId(),
            application.getApplicationCode(),
            "Application submitted successfully"
        );

        logger.info("Application submitted successfully: {}", application.getApplicationCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Save a university admission application without necessarily finishing the
     * workflow intake. Same create-or-update logic as {@link #submitApplication},
     * safe to call repeatedly for the same application (subsequent calls must pass
     * back {@code request.applicationId} from the previous response).
     *
     * @param request the application submission request with validated data
     * @return ResponseEntity containing the application number and success message
     */
    @PostMapping("/save")
    @PreAuthorize("hasRole('EXTERNAL_USER')")
    public ResponseEntity<ApplicationSubmissionResponse> saveApplication(
            @Valid @RequestBody ApplicationSubmissionRequest request) {

        logger.info("Received application save request: {}", request);

        Application application = processApplicationAndStartWorkflow(request);

        ApplicationSubmissionResponse response = new ApplicationSubmissionResponse(
            application.getId(),
            application.getApplicationCode(),
            "Application saved successfully"
        );

        logger.info("Application saved successfully: {}", application.getApplicationCode());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Shared by {@link #submitApplication} and {@link #saveApplication}: persists the
     * application (insert or update), then starts the jBPM process only if one hasn't
     * already been started for it.
     */
    private Application processApplicationAndStartWorkflow(ApplicationSubmissionRequest request) {
        try {
            // Get authenticated user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = "anonymous";

            if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticationFilter.UserPrincipal) {
                JwtAuthenticationFilter.UserPrincipal userPrincipal = (JwtAuthenticationFilter.UserPrincipal) authentication.getPrincipal();
                userId = userPrincipal.getUserId().toString();
                logger.info("Processing application request for user: {} ({})", userPrincipal.getEmail(), userId);
            }

            // Process the application create/update
            Application application = applicationSubmissionService.submitApplication(request, userId);

            // Only start a new process if one isn't already associated with this application
            // (RAP.WORKFLOW_APP_ASSOC, written by the processes module's ProcessEventListener)
            Long existingProcessInstanceId = applicationSubmissionService.findAssociatedProcessInstanceId(application.getId());
            if (existingProcessInstanceId == null) {
                Map<String, Object> processVars = new HashMap<>();
                processVars.put(Constants.DOMAIN_APPLICATION_STATUS, Constants.CD_APPLICATION_STATUS_PENDING);
                startProcess(ProcessInfo.NEW_SRP_APPLICATION, true, application, userId, processVars);
            } else {
                logger.info("Process already associated with application {} (processInstanceId={}), skipping",
                    application.getApplicationCode(), existingProcessInstanceId);
            }

            return application;

        } catch (IllegalArgumentException e) {
            logger.error("Invalid application data: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage());

        } catch (Exception e) {
            logger.error("Error processing application", e);
            throw new RuntimeException("Failed to process application: " + e.getMessage());
        }
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Exception handler for illegal arguments.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Exception handler for general errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        error.put("message", ex.getMessage());
        
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
