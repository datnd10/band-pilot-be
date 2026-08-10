package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a completed typing-test session.
 */
@Entity
@Table(name = "typing_session")
public class TypingSession {

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
            mappedBy = "typingSession",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<TypingSessionWordResult> wordResults = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected TypingSession() {
    }

    public TypingSession(Instant completedAt, int totalUniqueWords) {
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

    public List<TypingSessionWordResult> getWordResults() {
        return wordResults;
    }
}
