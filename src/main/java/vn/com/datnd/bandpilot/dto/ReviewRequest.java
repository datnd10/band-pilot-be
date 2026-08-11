package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ReviewRequest {

    @NotNull
    private UUID wordId;

    @NotNull
    private Rating rating;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ReviewRequest() {
    }

    public ReviewRequest(UUID wordId, Rating rating) {
        this.wordId = wordId;
        this.rating = rating;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }
}
