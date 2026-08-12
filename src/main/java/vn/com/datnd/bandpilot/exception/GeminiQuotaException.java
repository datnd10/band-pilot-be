package vn.com.datnd.bandpilot.exception;

public class GeminiQuotaException extends RuntimeException {

    public GeminiQuotaException(String message) {
        super(message);
    }

    public GeminiQuotaException(String message, Throwable cause) {
        super(message, cause);
    }
}
