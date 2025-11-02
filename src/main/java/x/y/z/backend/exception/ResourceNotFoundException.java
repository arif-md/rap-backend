package x.y.z.backend.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Handled by GlobalExceptionHandler to return 404 NOT FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " with identifier '" + identifier + "' not found");
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(resourceType + " with ID " + id + " not found");
    }
}
