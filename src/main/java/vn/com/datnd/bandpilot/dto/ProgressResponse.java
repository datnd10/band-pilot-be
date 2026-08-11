package vn.com.datnd.bandpilot.dto;

public class ProgressResponse {

    private long totalWords;
    private long dueToday;
    private long learnedWords;
    private long matureWords;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ProgressResponse() {
    }

    public ProgressResponse(long totalWords, long dueToday, long learnedWords, long matureWords) {
        this.totalWords = totalWords;
        this.dueToday = dueToday;
        this.learnedWords = learnedWords;
        this.matureWords = matureWords;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public long getTotalWords() {
        return totalWords;
    }

    public void setTotalWords(long totalWords) {
        this.totalWords = totalWords;
    }

    public long getDueToday() {
        return dueToday;
    }

    public void setDueToday(long dueToday) {
        this.dueToday = dueToday;
    }

    public long getLearnedWords() {
        return learnedWords;
    }

    public void setLearnedWords(long learnedWords) {
        this.learnedWords = learnedWords;
    }

    public long getMatureWords() {
        return matureWords;
    }

    public void setMatureWords(long matureWords) {
        this.matureWords = matureWords;
    }
}
