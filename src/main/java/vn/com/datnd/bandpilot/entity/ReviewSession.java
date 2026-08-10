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

    /** UTC timestamp when the session was completed. */
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "total_unique_words", nullable = false)
    private int totalUniqueWords;

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

    public ReviewSession(Instant completedAt, int totalUniqueWords) {
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
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

    public List<ReviewSessionWordResult> getWordResults() {
        return wordResults;
    }
}
