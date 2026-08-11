package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.com.datnd.bandpilot.dto.ReviewSessionRequest;
import vn.com.datnd.bandpilot.dto.ReviewSessionResponse;
import vn.com.datnd.bandpilot.dto.SessionDetailResponse;
import vn.com.datnd.bandpilot.dto.SessionHistorySummaryResponse;
import vn.com.datnd.bandpilot.dto.StreakResponse;
import vn.com.datnd.bandpilot.dto.TypingSessionRequest;
import vn.com.datnd.bandpilot.dto.TypingSessionResponse;
import vn.com.datnd.bandpilot.service.ReviewSessionService;
import vn.com.datnd.bandpilot.service.StreakService;
import vn.com.datnd.bandpilot.service.TypingSessionService;

import java.util.UUID;

/**
 * REST controller for saving session results and querying session history.
 *
 * <ul>
 *   <li>{@code POST /api/v1/sessions/review}           — persist a flashcard review session</li>
 *   <li>{@code POST /api/v1/sessions/typing}            — persist a typing-test session</li>
 *   <li>{@code GET  /api/v1/sessions/history}           — paginated session history list</li>
 *   <li>{@code GET  /api/v1/sessions/history/{id}}      — session detail by id</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewSessionService reviewSessionService;
    private final TypingSessionService typingSessionService;
    private final StreakService streakService;

    public SessionController(ReviewSessionService reviewSessionService,
                             TypingSessionService typingSessionService,
                             StreakService streakService) {
        this.reviewSessionService = reviewSessionService;
        this.typingSessionService = typingSessionService;
        this.streakService        = streakService;
    }

    // ── Save endpoints ────────────────────────────────────────────────────────────

    /**
     * Save a completed flashcard review session and update word statuses.
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
     * @param request session results from the client
     * @return the saved session metadata with HTTP 201
     */
    @PostMapping("/typing")
    @ResponseStatus(HttpStatus.CREATED)
    public TypingSessionResponse saveTypingSession(@Valid @RequestBody TypingSessionRequest request) {
        return typingSessionService.saveTypingSession(request);
    }

    // ── History endpoints ─────────────────────────────────────────────────────────

    // ── Streak endpoint ───────────────────────────────────────────────────────────

    /**
     * Returns the current user's streak and badge data.
     *
     * @param auth injected by Spring Security from the JWT token
     * @return streak summary with HTTP 200
     */
    @GetMapping("/streak")
    public ResponseEntity<StreakResponse> getStreak(Authentication auth) {
        UUID userId = resolveUserId(auth);
        return ResponseEntity.ok(streakService.getStreak(userId));
    }

    /**
     * Returns a paginated list of the current user's review sessions, ordered by
     * completedAt descending (newest first).
     *
     * @param page  0-indexed page number (default 0)
     * @param size  page size (default 20, capped at 100)
     * @param auth  injected by Spring Security from the JWT token
     * @return page of session summaries
     */
    @GetMapping("/history")
    public ResponseEntity<Page<SessionHistorySummaryResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        int effectiveSize = Math.min(size, MAX_PAGE_SIZE);
        UUID userId = resolveUserId(auth);
        Page<SessionHistorySummaryResponse> result =
                reviewSessionService.getSessionHistory(userId, page, effectiveSize);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns the full detail of a single session, including per-word results.
     * Returns 404 if the session does not exist or belongs to a different user.
     *
     * @param id   session UUID from path
     * @param auth injected by Spring Security from the JWT token
     * @return session detail
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<SessionDetailResponse> getDetail(
            @PathVariable UUID id,
            Authentication auth) {

        UUID userId = resolveUserId(auth);
        SessionDetailResponse detail = reviewSessionService.getSessionDetail(id, userId);
        return ResponseEntity.ok(detail);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Derives a deterministic UUID from the JWT principal (username).
     * Delegates to the same logic in ReviewSessionService so userId is consistent.
     */
    private UUID resolveUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return ReviewSessionService.NIL_UUID;
        }
        String principal = auth.getPrincipal().toString();
        return UUID.nameUUIDFromBytes(principal.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
