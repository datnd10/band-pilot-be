package vn.com.datnd.bandpilot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReviewResponse {

    private UUID wordId;
    private LocalDate nextReviewDate;
    private int interval;
    private BigDecimal easeFactor;
    private int repetitions;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ReviewResponse() {
    }

    public ReviewResponse(UUID wordId, LocalDate nextReviewDate, int interval,
                          BigDecimal easeFactor, int repetitions) {
        this.wordId = wordId;
        this.nextReviewDate = nextReviewDate;
        this.interval = interval;
        this.easeFactor = easeFactor;
        this.repetitions = repetitions;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
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
}
