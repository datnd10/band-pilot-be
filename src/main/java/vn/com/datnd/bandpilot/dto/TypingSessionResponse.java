package vn.com.datnd.bandpilot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload returned after saving a typing session.
 */
public class TypingSessionResponse {

    private UUID id;
    private Instant completedAt;
    private int totalUniqueWords;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public TypingSessionResponse() {
    }

    public TypingSessionResponse(UUID id, Instant completedAt, int totalUniqueWords) {
        this.id = id;
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getTotalUniqueWords() {
        return totalUniqueWords;
    }

    public void setTotalUniqueWords(int totalUniqueWords) {
        this.totalUniqueWords = totalUniqueWords;
    }
}
