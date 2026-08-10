package vn.com.datnd.bandpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for saving a completed flashcard review session.
 *
 * <pre>{@code
 * {
 *   "results": [
 *     { "wordId": "uuid",  "unknownCount": 0 },
 *     { "wordId": "uuid2", "unknownCount": 2 }
 *   ]
 * }
 * }</pre>
 */
public class ReviewSessionRequest {

    @NotNull(message = "results must not be null")
    @Valid
    private List<WordResult> results;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public ReviewSessionRequest() {
    }

    public ReviewSessionRequest(List<WordResult> results) {
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
     * Per-word result within a review session.
     */
    public static class WordResult {

        @NotNull(message = "wordId must not be null")
        private UUID wordId;

        @Min(value = 0, message = "unknownCount must be >= 0")
        private int unknownCount;

        // ── Constructors ──────────────────────────────────────────────────────────

        public WordResult() {
        }

        public WordResult(UUID wordId, int unknownCount) {
            this.wordId = wordId;
            this.unknownCount = unknownCount;
        }

        // ── Getters & Setters ─────────────────────────────────────────────────────

        public UUID getWordId() {
            return wordId;
        }

        public void setWordId(UUID wordId) {
            this.wordId = wordId;
        }

        public int getUnknownCount() {
            return unknownCount;
        }

        public void setUnknownCount(int unknownCount) {
            this.unknownCount = unknownCount;
        }
    }
}
