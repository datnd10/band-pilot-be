package vn.com.datnd.bandpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for saving a completed typing-test session.
 *
 * <pre>{@code
 * {
 *   "results": [
 *     { "wordId": "uuid",  "attemptsRequired": 1 },
 *     { "wordId": "uuid2", "attemptsRequired": 3 }
 *   ]
 * }
 * }</pre>
 */
public class TypingSessionRequest {

    @NotNull(message = "results must not be null")
    @Valid
    private List<WordResult> results;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public TypingSessionRequest() {
    }

    public TypingSessionRequest(List<WordResult> results) {
        this.results = results;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public List<WordResult> getResults() {
        return results;
    }

    public void setResults(List<WordResult> results) {
        this.results = results;
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────────

    /**
     * Per-word result within a typing session.
     */
    public static class WordResult {

        @NotNull(message = "wordId must not be null")
        private UUID wordId;

        @Min(value = 1, message = "attemptsRequired must be >= 1")
        private int attemptsRequired;

        // ── Constructors ──────────────────────────────────────────────────────────

        public WordResult() {
        }

        public WordResult(UUID wordId, int attemptsRequired) {
            this.wordId = wordId;
            this.attemptsRequired = attemptsRequired;
        }

        // ── Getters & Setters ─────────────────────────────────────────────────────

        public UUID getWordId() {
            return wordId;
        }

        public void setWordId(UUID wordId) {
            this.wordId = wordId;
        }

        public int getAttemptsRequired() {
            return attemptsRequired;
        }

        public void setAttemptsRequired(int attemptsRequired) {
            this.attemptsRequired = attemptsRequired;
        }
    }
}
