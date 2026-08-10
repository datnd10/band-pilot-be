package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.com.datnd.bandpilot.dto.ReviewSessionRequest;
import vn.com.datnd.bandpilot.dto.ReviewSessionResponse;
import vn.com.datnd.bandpilot.dto.TypingSessionRequest;
import vn.com.datnd.bandpilot.dto.TypingSessionResponse;
import vn.com.datnd.bandpilot.service.ReviewSessionService;
import vn.com.datnd.bandpilot.service.TypingSessionService;

/**
 * REST controller for saving session results (flashcard review and typing test).
 *
 * <ul>
 *   <li>{@code POST /api/v1/sessions/review} — persist a flashcard review session (Requirements 4.1)</li>
 *   <li>{@code POST /api/v1/sessions/typing} — persist a typing-test session (Requirement 8.7)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final ReviewSessionService reviewSessionService;
    private final TypingSessionService typingSessionService;

    public SessionController(ReviewSessionService reviewSessionService,
                             TypingSessionService typingSessionService) {
        this.reviewSessionService = reviewSessionService;
        this.typingSessionService = typingSessionService;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────────

    /**
     * Save a completed flashcard review session and update word statuses.
     *
     * <p>Status transition rules:
     * <ul>
     *   <li>{@code unknownCount == 0} → word status set to {@code "Known"}</li>
     *   <li>{@code unknownCount >= 1} → word status set to {@code "Learning"}</li>
     * </ul>
     * </p>
     *
     * @param request session results from the client
     * @return the saved session metadata with HTTP 201
     */
    @PostMapping("/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewSessionResponse saveReviewSession(@Valid @RequestBody ReviewSessionRequest request) {
        return reviewSessionService.saveReviewSession(request);
    }

    /**
     * Save a completed typing-test session and update word statuses.
     *
     * <p>Status transition rules:
     * <ul>
     *   <li>{@code attemptsRequired == 1} → word status set to {@code "Known"}</li>
     *   <li>{@code attemptsRequired > 1}  → word status set to {@code "Learning"} (including downgrade from {@code "Known"})</li>
     * </ul>
     * </p>
     *
     * @param request session results from the client
     * @return the saved session metadata with HTTP 201
     */
    @PostMapping("/typing")
    @ResponseStatus(HttpStatus.CREATED)
    public TypingSessionResponse saveTypingSession(@Valid @RequestBody TypingSessionRequest request) {
        return typingSessionService.saveTypingSession(request);
    }
}
