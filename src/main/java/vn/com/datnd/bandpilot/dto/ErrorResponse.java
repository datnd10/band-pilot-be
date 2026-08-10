package vn.com.datnd.bandpilot.dto;

/**
 * Standardised JSON error envelope returned by the global exception handler.
 * <pre>
 * {
 *   "status":    400,
 *   "error":     "Bad Request",
 *   "message":   "English word is required",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp
) {
}
