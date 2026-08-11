package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "srs_record")
public class SrsRecord {

    @Id
    @Column(name = "word_id", nullable = false, updatable = false)
    private UUID wordId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id")
    private WordEntry wordEntry;

    @Column(name = "interval", nullable = false)
    private int interval = 1;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    @Column(name = "repetitions", nullable = false)
    private int repetitions = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected SrsRecord() {
    }

    public SrsRecord(UUID wordId, WordEntry wordEntry, int interval,
                     BigDecimal easeFactor, int repetitions, LocalDate nextReviewDate) {
        this.wordId = wordId;
        this.wordEntry = wordEntry;
        this.interval = interval;
        this.easeFactor = easeFactor;
        this.repetitions = repetitions;
        this.nextReviewDate = nextReviewDate;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }

    public WordEntry getWordEntry() {
        return wordEntry;
    }

    public void setWordEntry(WordEntry wordEntry) {
        this.wordEntry = wordEntry;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }
}
