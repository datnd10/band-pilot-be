package vn.com.datnd.bandpilot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload for a vocabulary group.
 */
public class GroupResponse {

    private UUID id;
    private String name;
    private int wordCount;
    private Instant createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public GroupResponse() {
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
