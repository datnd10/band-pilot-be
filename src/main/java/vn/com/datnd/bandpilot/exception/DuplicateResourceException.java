package vn.com.datnd.bandpilot.exception;

/**
 * Thrown when attempting to create a resource that already exists
 * (e.g. duplicate English word, duplicate group name, word already in group).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
