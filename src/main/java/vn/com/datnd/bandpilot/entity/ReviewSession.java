package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a completed flashcard review session.
 */
@Entity
@Table(name = "review_session")
public class ReviewSession {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** UTC timestamp when the session was started. */
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** UTC timestamp when the session was completed. */
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "total_unique_words", nullable = false)
    private int totalUniqueWords;

    /** Owner of this session. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(
            mappedBy = "reviewSession",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ReviewSessionWordResult> wordResults = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected ReviewSession() {
    }

    /** Legacy constructor — kept for backward compatibility. userId will be nil UUID. */
    public ReviewSession(Instant completedAt, int totalUniqueWords) {
        this.startedAt = completedAt;
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
        this.userId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    /** Full constructor used by new save path. */
    public ReviewSession(Instant startedAt, Instant completedAt, int totalUniqueWords, UUID userId) {
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
        this.userId = userId;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<ReviewSessionWordResult> getWordResults() {
        return wordResults;
    }
}
