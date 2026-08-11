package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.TypingSessionRequest;
import vn.com.datnd.bandpilot.dto.TypingSessionResponse;
import vn.com.datnd.bandpilot.entity.TypingSession;
import vn.com.datnd.bandpilot.entity.TypingSessionWordResult;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.repository.TypingSessionRepository;
import vn.com.datnd.bandpilot.repository.TypingSessionWordResultRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;
import vn.com.datnd.bandpilot.service.ReviewSessionService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for saving typing-test sessions and updating word statuses.
 *
 * <p>Status transition rules (Requirement 8.7):
 * <ul>
 *   <li>{@code attemptsRequired == 1} → status set to {@code "Known"}</li>
 *   <li>{@code attemptsRequired > 1}  → status set to {@code "Learning"} (including downgrade from {@code "Known"})</li>
 *   <li>Words absent from the session result: status left unchanged</li>
 * </ul>
 * </p>
 */
@Service
public class TypingSessionService {

    private static final Logger log = LoggerFactory.getLogger(TypingSessionService.class);

    private static final String STATUS_KNOWN    = "Known";
    private static final String STATUS_LEARNING = "Learning";

    private final TypingSessionRepository            sessionRepository;
    private final TypingSessionWordResultRepository  wordResultRepository;
    private final WordEntryRepository                wordEntryRepository;

    public TypingSessionService(TypingSessionRepository sessionRepository,
                                TypingSessionWordResultRepository wordResultRepository,
                                WordEntryRepository wordEntryRepository) {
        this.sessionRepository    = sessionRepository;
        this.wordResultRepository = wordResultRepository;
        this.wordEntryRepository  = wordEntryRepository;
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Persists a completed typing session and applies word-status transitions.
     *
     * <ol>
     *   <li>Creates and saves a {@link TypingSession} with the current UTC time.</li>
     *   <li>For each {@link TypingSessionRequest.WordResult}, creates a
     *       {@link TypingSessionWordResult} row linked to the session and word.</li>
     *   <li>Updates each {@link WordEntry} status according to the transition rules.</li>
     * </ol>
     *
     * @param request the session results submitted by the client
     * @return a {@link TypingSessionResponse} containing the saved session's metadata
     */
    @Transactional
    public TypingSessionResponse saveTypingSession(TypingSessionRequest request) {
        // 1. Persist the session header
        UUID userId = ReviewSessionService.resolveUserId();
        TypingSession session = new TypingSession(
                Instant.now(),
                request.getResults().size(),
                userId
        );
        session = sessionRepository.save(session);

        // 2. Persist per-word results and update statuses
        for (TypingSessionRequest.WordResult wordResult : request.getResults()) {
            Optional<WordEntry> wordEntryOpt = wordEntryRepository.findById(wordResult.getWordId());

            if (wordEntryOpt.isEmpty()) {
                log.warn("Word not found for id={}, skipping status update", wordResult.getWordId());
                continue;
            }

            WordEntry wordEntry = wordEntryOpt.get();

            // 2a. Save the result row
            TypingSessionWordResult result = new TypingSessionWordResult(
                    session,
                    wordEntry,
                    wordResult.getAttemptsRequired()
            );
            wordResultRepository.save(result);

            // 2b. Apply status transition (Requirement 8.7)
            String newStatus = wordResult.getAttemptsRequired() == 1 ? STATUS_KNOWN : STATUS_LEARNING;
            wordEntry.setStatus(newStatus);
            wordEntryRepository.save(wordEntry);
        }

        return toResponse(session);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private TypingSessionResponse toResponse(TypingSession session) {
        return new TypingSessionResponse(
                session.getId(),
                session.getCompletedAt(),
                session.getTotalUniqueWords()
        );
    }
}
