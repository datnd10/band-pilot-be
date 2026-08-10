package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Records how many times a word was marked "Unknown" during a {@link ReviewSession}.
 */
@Entity
@Table(name = "review_session_word_result")
public class ReviewSessionWordResult {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ReviewSession reviewSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntry wordEntry;

    /** Number of times the user marked this word as Unknown (≥ 0). */
    @Column(name = "unknown_count", nullable = false)
    private int unknownCount;

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected ReviewSessionWordResult() {
    }

    public ReviewSessionWordResult(ReviewSession reviewSession, WordEntry wordEntry, int unknownCount) {
        this.reviewSession = reviewSession;
        this.wordEntry = wordEntry;
        this.unknownCount = unknownCount;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public ReviewSession getReviewSession() {
        return reviewSession;
    }

    public void setReviewSession(ReviewSession reviewSession) {
        this.reviewSession = reviewSession;
    }

    public WordEntry getWordEntry() {
        return wordEntry;
    }

    public void setWordEntry(WordEntry wordEntry) {
        this.wordEntry = wordEntry;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(int unknownCount) {
        this.unknownCount = unknownCount;
    }
}
