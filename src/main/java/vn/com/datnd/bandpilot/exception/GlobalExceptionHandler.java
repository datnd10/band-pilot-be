package vn.com.datnd.bandpilot.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.com.datnd.bandpilot.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Centrally maps domain exceptions to the standardised JSON error envelope:
 * {@code { "status", "error", "message", "timestamp" }}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 502 Bad Gateway (Gemini unavailable) ─────────────────────────────────────

    @ExceptionHandler(GeminiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGeminiUnavailable(GeminiUnavailableException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    // ── 503 Service Unavailable (Gemini quota/auth) ───────────────────────────────

    @ExceptionHandler(GeminiQuotaException.class)
    public ResponseEntity<ErrorResponse> handleGeminiQuota(GeminiQuotaException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    // ── 404 Not Found ────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ── 404 Static resource (favicon, etc.) ──────────────────────────────────────

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoStaticResource(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        // Browser auto-requests favicon.ico etc — suppress to WARN, not ERROR
        log.debug("Static resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not found");
    }

    // ── 409 Conflict ─────────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    // ── 400 Bad Request (domain validation) ──────────────────────────────────────

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ExampleLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleExampleLimit(ExampleLimitExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ── 400 Bad Request (Spring Bean Validation) ──────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    // ── 500 Internal Server Error (fallback) ─────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Suppress noisy favicon/static resource errors
        String message = ex.getMessage();
        if (message != null && message.contains("favicon")) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not found");
        }
        log.error("Unexpected error: {}", message, ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        String timestamp = LocalDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        ErrorResponse body = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                timestamp
        );
        return ResponseEntity.status(status).body(body);
    }
}
