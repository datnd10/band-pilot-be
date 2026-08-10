package vn.com.datnd.bandpilot.exception;

/**
 * Thrown when domain-level validation fails (e.g. missing required field,
 * field value exceeds length limit, invalid CSV format).
 * Maps to HTTP 400 Bad Request.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
