package vn.com.datnd.bandpilot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary of a single flashcard review session, used in the session history list.
 */
public class SessionHistorySummaryResponse {

    private UUID id;
    private Instant startedAt;
    private Instant completedAt;
    private int totalUniqueWords;
    private long easyCount;
    private long goodCount;
    private long againCount;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public SessionHistorySummaryResponse() {
    }

    public SessionHistorySummaryResponse(UUID id, Instant startedAt, Instant completedAt,
                                         int totalUniqueWords,
                                         long easyCount, long goodCount, long againCount) {
        this.id = id;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
        this.easyCount = easyCount;
        this.goodCount = goodCount;
        this.againCount = againCount;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public int getTotalUniqueWords() { return totalUniqueWords; }
    public void setTotalUniqueWords(int totalUniqueWords) { this.totalUniqueWords = totalUniqueWords; }

    public long getEasyCount() { return easyCount; }
    public void setEasyCount(long easyCount) { this.easyCount = easyCount; }

    public long getGoodCount() { return goodCount; }
    public void setGoodCount(long goodCount) { this.goodCount = goodCount; }

    public long getAgainCount() { return againCount; }
    public void setAgainCount(long againCount) { this.againCount = againCount; }
}
