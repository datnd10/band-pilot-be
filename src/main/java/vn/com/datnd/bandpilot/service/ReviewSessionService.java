package vn.com.datnd.bandpilot.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.Rating;
import vn.com.datnd.bandpilot.dto.ReviewSessionRequest;
import vn.com.datnd.bandpilot.dto.ReviewSessionResponse;
import vn.com.datnd.bandpilot.dto.SessionDetailResponse;
import vn.com.datnd.bandpilot.dto.SessionHistorySummaryResponse;
import vn.com.datnd.bandpilot.entity.ReviewSession;
import vn.com.datnd.bandpilot.entity.ReviewSessionWordResult;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.repository.ReviewSessionRepository;
import vn.com.datnd.bandpilot.repository.ReviewSessionWordResultRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for saving flashcard review sessions, updating word statuses,
 * and querying session history.
 *
 * <p>Status transition rules (Requirements 4.1, 4.2):
 * <ul>
 *   <li>{@code unknownCount == 0} → status set to {@code "Known"}</li>
 *   <li>{@code unknownCount >= 1} → status set to {@code "Learning"}</li>
 *   <li>Words absent from the session result: status left unchanged</li>
 * </ul>
 * </p>
 *
 * <p>Rating mapping from unknownCount:
 * <ul>
 *   <li>0 → EASY</li>
 *   <li>1 → GOOD</li>
 *   <li>≥ 2 → AGAIN</li>
 * </ul>
 * </p>
 */
@Service
public class ReviewSessionService {

    private static final String STATUS_KNOWN    = "Known";
    private static final String STATUS_LEARNING = "Learning";

    /** Nil UUID used for the single-user app (legacy / backfill). */
    public static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ReviewSessionRepository            sessionRepository;
    private final ReviewSessionWordResultRepository  wordResultRepository;
    private final WordEntryRepository                wordEntryRepository;

    public ReviewSessionService(ReviewSessionRepository sessionRepository,
                                ReviewSessionWordResultRepository wordResultRepository,
                                WordEntryRepository wordEntryRepository) {
        this.sessionRepository    = sessionRepository;
        this.wordResultRepository = wordResultRepository;
        this.wordEntryRepository  = wordEntryRepository;
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Persists a completed review session and applies word-status transitions.
     *
     * <ol>
     *   <li>Creates and saves a {@link ReviewSession} with startedAt/completedAt (current UTC time)
     *       and userId derived from the JWT principal (username → deterministic UUID).</li>
     *   <li>For each {@link ReviewSessionRequest.WordResult}, creates a
     *       {@link ReviewSessionWordResult} row with both {@code unknownCount} and derived
     *       {@code rating}.</li>
     *   <li>Updates each {@link WordEntry} status according to the transition rules.</li>
     * </ol>
     *
     * @param request the session results submitted by the client
     * @return a {@link ReviewSessionResponse} containing the saved session's metadata
     * @throws ResourceNotFoundException if any {@code wordId} in the request does not exist
     */
    @Transactional
    public ReviewSessionResponse saveReviewSession(ReviewSessionRequest request) {
        Instant now = Instant.now();
        UUID userId = resolveUserId();

        // 1. Persist the session header
        ReviewSession session = new ReviewSession(now, now, request.getResults().size(), userId);
        session = sessionRepository.save(session);

        // 2. Persist per-word results and update statuses
        for (ReviewSessionRequest.WordResult wordResult : request.getResults()) {
            WordEntry wordEntry = wordEntryRepository.findById(wordResult.getWordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Word", wordResult.getWordId()));

            // 2a. Derive rating from unknownCount: 0→EASY, 1→GOOD, ≥2→AGAIN
            Rating rating = ReviewSessionWordResult.deriveRating(wordResult.getUnknownCount());

            // 2b. Save the result row (with both unknownCount and rating)
            ReviewSessionWordResult result = new ReviewSessionWordResult(
                    session,
                    wordEntry,
                    wordResult.getUnknownCount(),
                    rating
            );
            wordResultRepository.save(result);

            // 2c. Apply status transition
            String newStatus = wordResult.getUnknownCount() == 0 ? STATUS_KNOWN : STATUS_LEARNING;
            wordEntry.setStatus(newStatus);
            wordEntryRepository.save(wordEntry);
        }

        return toResponse(session);
    }

    /**
     * Returns a page of session summaries for the given user, ordered by completedAt DESC.
     *
     * @param userId the user whose sessions to query
     * @param page   0-indexed page number
     * @param size   page size (caller should cap at 100)
     * @return paged session summaries
     */
    @Transactional(readOnly = true)
    public Page<SessionHistorySummaryResponse> getSessionHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewSession> sessions = sessionRepository.findByUserIdOrderByCompletedAtDesc(userId, pageable);
        return sessions.map(this::toSummaryResponse);
    }

    /**
     * Returns the full detail of a session, including per-word results.
     *
     * @param sessionId the session UUID
     * @param userId    the requesting user — must match session owner
     * @return session detail
     * @throws ResourceNotFoundException if session does not exist or belongs to a different user
     */
    @Transactional(readOnly = true)
    public SessionDetailResponse getSessionDetail(UUID sessionId, UUID userId) {
        ReviewSession session = sessionRepository.findByIdWithWordResults(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (!session.getUserId().equals(userId)) {
            // Do not leak that the session exists — return 404
            throw new ResourceNotFoundException("Session", sessionId);
        }

        return toDetailResponse(session);
    }

    // ── Helper: resolve userId from JWT principal ─────────────────────────────────

    /**
     * Derives a deterministic UUID from the JWT principal (username).
     * Uses UUID v3 (name-based) with a fixed namespace for reproducibility.
     * Single-user apps will always get the same UUID for "admin".
     */
    public static UUID resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return NIL_UUID;
        }
        String principal = auth.getPrincipal().toString();
        // Derive a stable UUID from the username string
        return UUID.nameUUIDFromBytes(principal.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────────

    private ReviewSessionResponse toResponse(ReviewSession session) {
        return new ReviewSessionResponse(
                session.getId(),
                session.getCompletedAt(),
                session.getTotalUniqueWords()
        );
    }

    private SessionHistorySummaryResponse toSummaryResponse(ReviewSession session) {
        List<ReviewSessionWordResult> results = session.getWordResults();
        long easyCount  = results.stream().filter(r -> r.getRating() == Rating.EASY).count();
        long goodCount  = results.stream().filter(r -> r.getRating() == Rating.GOOD).count();
        long againCount = results.stream().filter(r -> r.getRating() == Rating.AGAIN).count();

        return new SessionHistorySummaryResponse(
                session.getId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getTotalUniqueWords(),
                easyCount,
                goodCount,
                againCount
        );
    }

    private SessionDetailResponse toDetailResponse(ReviewSession session) {
        List<ReviewSessionWordResult> results = session.getWordResults();

        long easyCount  = results.stream().filter(r -> r.getRating() == Rating.EASY).count();
        long goodCount  = results.stream().filter(r -> r.getRating() == Rating.GOOD).count();
        long againCount = results.stream().filter(r -> r.getRating() == Rating.AGAIN).count();

        List<SessionDetailResponse.WordResultItem> wordResultItems = results.stream()
                .map(r -> new SessionDetailResponse.WordResultItem(
                        r.getWordEntry().getId(),
                        r.getWordEntry().getWord(),
                        r.getRating().name()
                ))
                .collect(Collectors.toList());

        return new SessionDetailResponse(
                session.getId(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getTotalUniqueWords(),
                easyCount,
                goodCount,
                againCount,
                wordResultItems
        );
    }
}
