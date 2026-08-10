package vn.com.datnd.bandpilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response payload for a single vocabulary group, including its word list.
 *
 * <p>Returned by {@code GET /api/v1/groups/{id}}.</p>
 *
 * <p>Requirement 7.4</p>
 */
public class GroupDetailResponse {

    private UUID id;
    private String name;
    private int wordCount;
    private Instant createdAt;
    private List<WordResponse> words;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public GroupDetailResponse() {
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

    public List<WordResponse> getWords() {
        return words;
    }

    public void setWords(List<WordResponse> words) {
        this.words = words;
    }
}
