package x.y.z.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import x.y.z.backend.controller.dto.ApplicationSubmissionRequest;
import x.y.z.backend.controller.dto.ApplicationSubmissionResponse;
import x.y.z.backend.domain.model.Application;
import x.y.z.backend.security.JwtAuthenticationFilter;
import x.y.z.backend.service.ApplicationSubmissionService;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for handling university admission application submissions.
 * Requires authentication for all endpoints.
 */
@RestController
@RequestMapping("/api/applications/submissions")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class ApplicationSubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSubmissionController.class);

    private final ApplicationSubmissionService applicationSubmissionService;

    public ApplicationSubmissionController(ApplicationSubmissionService applicationSubmissionService) {
        this.applicationSubmissionService = applicationSubmissionService;
    }

    /**
     * Submit a new university admission application.
     * 
     * @param request the application submission request with validated data
     * @return ResponseEntity containing the application number and success message
     */
    @PostMapping
    public ResponseEntity<ApplicationSubmissionResponse> submitApplication(
            @Valid @RequestBody ApplicationSubmissionRequest request) {
        
        logger.info("Received application submission request: {}", request);

        try {
            // Get authenticated user ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = "anonymous";
            
            if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticationFilter.UserPrincipal) {
                JwtAuthenticationFilter.UserPrincipal userPrincipal = (JwtAuthenticationFilter.UserPrincipal) authentication.getPrincipal();
                userId = userPrincipal.getUserId().toString();
                logger.info("Processing application submission for user: {} ({})", userPrincipal.getEmail(), userId);
            }

            // Process the application submission
            Application createdApplication = applicationSubmissionService.submitApplication(request, userId);

            // Create success response
            ApplicationSubmissionResponse response = new ApplicationSubmissionResponse(
                createdApplication.getId(),
                createdApplication.getApplicationCode(),
                "Application submitted successfully"
            );

            logger.info("Application submitted successfully: {}", createdApplication.getApplicationCode());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid application data: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
            
        } catch (Exception e) {
            logger.error("Error submitting application", e);
            throw new RuntimeException("Failed to submit application: " + e.getMessage());
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
