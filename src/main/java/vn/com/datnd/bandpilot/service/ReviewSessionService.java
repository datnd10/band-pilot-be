package vn.com.datnd.bandpilot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.ReviewSessionRequest;
import vn.com.datnd.bandpilot.dto.ReviewSessionResponse;
import vn.com.datnd.bandpilot.entity.ReviewSession;
import vn.com.datnd.bandpilot.entity.ReviewSessionWordResult;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.repository.ReviewSessionRepository;
import vn.com.datnd.bandpilot.repository.ReviewSessionWordResultRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;

import java.time.Instant;

/**
 * Business logic for saving flashcard review sessions and updating word statuses.
 *
 * <p>Status transition rules (Requirements 4.1, 4.2):
 * <ul>
 *   <li>{@code unknownCount == 0} → status set to {@code "Known"}</li>
 *   <li>{@code unknownCount >= 1} → status set to {@code "Learning"}</li>
 *   <li>Words absent from the session result: status left unchanged</li>
 * </ul>
 * </p>
 */
@Service
public class ReviewSessionService {

    private static final String STATUS_KNOWN    = "Known";
    private static final String STATUS_LEARNING = "Learning";

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
     *   <li>Creates and saves a {@link ReviewSession} with the current UTC time.</li>
     *   <li>For each {@link ReviewSessionRequest.WordResult}, creates a
     *       {@link ReviewSessionWordResult} row linked to the session and word.</li>
     *   <li>Updates each {@link WordEntry} status according to the transition rules.</li>
     * </ol>
     *
     * @param request the session results submitted by the client
     * @return a {@link ReviewSessionResponse} containing the saved session's metadata
     * @throws ResourceNotFoundException if any {@code wordId} in the request does not exist
     */
    @Transactional
    public ReviewSessionResponse saveReviewSession(ReviewSessionRequest request) {
        // 1. Persist the session header
        ReviewSession session = new ReviewSession(
                Instant.now(),
                request.getResults().size()
        );
        session = sessionRepository.save(session);

        // 2. Persist per-word results and update statuses
        for (ReviewSessionRequest.WordResult wordResult : request.getResults()) {
            WordEntry wordEntry = wordEntryRepository.findById(wordResult.getWordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Word", wordResult.getWordId()));

            // 2a. Save the result row
            ReviewSessionWordResult result = new ReviewSessionWordResult(
                    session,
                    wordEntry,
                    wordResult.getUnknownCount()
            );
            wordResultRepository.save(result);

            // 2b. Apply status transition
            String newStatus = wordResult.getUnknownCount() == 0 ? STATUS_KNOWN : STATUS_LEARNING;
            wordEntry.setStatus(newStatus);
            wordEntryRepository.save(wordEntry);
        }

        return toResponse(session);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private ReviewSessionResponse toResponse(ReviewSession session) {
        return new ReviewSessionResponse(
                session.getId(),
                session.getCompletedAt(),
                session.getTotalUniqueWords()
        );
    }
}
