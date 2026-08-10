package vn.com.datnd.bandpilot.exception;

/**
 * Thrown when attempting to add a 4th example sentence to a WordEntry
 * that already has the maximum of 3 examples.
 * Maps to HTTP 400 Bad Request.
 */
public class ExampleLimitExceededException extends RuntimeException {

    private static final int MAX_EXAMPLES = 3;

    public ExampleLimitExceededException() {
        super("Cannot add more than " + MAX_EXAMPLES + " example sentences per word.");
    }

    public ExampleLimitExceededException(String message) {
        super(message);
    }
}
