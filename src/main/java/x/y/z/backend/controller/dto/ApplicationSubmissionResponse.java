package x.y.z.backend.controller.dto;

/**
 * DTO for application submission response.
 * Contains the generated application code/number.
 */
public class ApplicationSubmissionResponse {

    private Long applicationId;
    private String applicationCode;
    private String message;

    // Constructors
    public ApplicationSubmissionResponse() {
    }

    public ApplicationSubmissionResponse(Long applicationId, String applicationCode, String message) {
        this.applicationId = applicationId;
        this.applicationCode = applicationCode;
        this.message = message;
    }

    // Getters and Setters
    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ApplicationSubmissionResponse{" +
                "applicationId=" + applicationId +
                ", applicationCode='" + applicationCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
