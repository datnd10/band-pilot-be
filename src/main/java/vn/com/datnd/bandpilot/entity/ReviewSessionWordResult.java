package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import vn.com.datnd.bandpilot.dto.Rating;

import java.util.UUID;

/**
 * Records how many times a word was marked "Unknown" during a {@link ReviewSession},
 * and the derived rating (EASY / GOOD / AGAIN).
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

    /** Number of times the user marked this word as Unknown (≥ 0). Kept for backward compatibility. */
    @Column(name = "unknown_count", nullable = false)
    private int unknownCount;

    /** Derived rating from unknownCount: 0 → EASY, 1 → GOOD, ≥ 2 → AGAIN. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false, length = 10)
    private Rating rating;

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected ReviewSessionWordResult() {
    }

    /** Legacy constructor — rating is auto-derived from unknownCount. */
    public ReviewSessionWordResult(ReviewSession reviewSession, WordEntry wordEntry, int unknownCount) {
        this.reviewSession = reviewSession;
        this.wordEntry = wordEntry;
        this.unknownCount = unknownCount;
        this.rating = deriveRating(unknownCount);
    }

    /** Full constructor with explicit rating. */
    public ReviewSessionWordResult(ReviewSession reviewSession, WordEntry wordEntry,
                                   int unknownCount, Rating rating) {
        this.reviewSession = reviewSession;
        this.wordEntry = wordEntry;
        this.unknownCount = unknownCount;
        this.rating = rating;
    }

    // ── Rating derivation ─────────────────────────────────────────────────────────

    /**
     * Maps unknownCount to a Rating:
     * 0 → EASY, 1 → GOOD, ≥ 2 → AGAIN
     */
    public static Rating deriveRating(int unknownCount) {
        if (unknownCount == 0) return Rating.EASY;
        if (unknownCount == 1) return Rating.GOOD;
        return Rating.AGAIN;
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

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }
}
