package vn.com.datnd.bandpilot.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed view of a single flashcard review session, including per-word results.
 */
public class SessionDetailResponse {

    private UUID id;
    private Instant startedAt;
    private Instant completedAt;
    private int totalUniqueWords;
    private long easyCount;
    private long goodCount;
    private long againCount;
    private List<WordResultItem> wordResults;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public SessionDetailResponse() {
    }

    public SessionDetailResponse(UUID id, Instant startedAt, Instant completedAt,
                                  int totalUniqueWords,
                                  long easyCount, long goodCount, long againCount,
                                  List<WordResultItem> wordResults) {
        this.id = id;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalUniqueWords = totalUniqueWords;
        this.easyCount = easyCount;
        this.goodCount = goodCount;
        this.againCount = againCount;
        this.wordResults = wordResults;
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

    public List<WordResultItem> getWordResults() { return wordResults; }
    public void setWordResults(List<WordResultItem> wordResults) { this.wordResults = wordResults; }

    // ── Inner DTO ─────────────────────────────────────────────────────────────────

    /**
     * Per-word result within a session detail response.
     */
    public static class WordResultItem {

        private UUID wordId;
        private String word;   // WordEntry.word text
        private String rating; // "EASY" | "GOOD" | "AGAIN"

        public WordResultItem() {
        }

        public WordResultItem(UUID wordId, String word, String rating) {
            this.wordId = wordId;
            this.word = word;
            this.rating = rating;
        }

        public UUID getWordId() { return wordId; }
        public void setWordId(UUID wordId) { this.wordId = wordId; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getRating() { return rating; }
        public void setRating(String rating) { this.rating = rating; }
    }
}
