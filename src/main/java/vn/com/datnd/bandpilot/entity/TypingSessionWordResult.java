package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Records how many attempts were required to correctly type a word during a {@link TypingSession}.
 */
@Entity
@Table(name = "typing_session_word_result")
public class TypingSessionWordResult {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TypingSession typingSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntry wordEntry;

    /** Number of typing attempts required before a correct answer (≥ 1). */
    @Column(name = "attempts_required", nullable = false)
    private int attemptsRequired;

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected TypingSessionWordResult() {
    }

    public TypingSessionWordResult(TypingSession typingSession, WordEntry wordEntry, int attemptsRequired) {
        this.typingSession = typingSession;
        this.wordEntry = wordEntry;
        this.attemptsRequired = attemptsRequired;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public TypingSession getTypingSession() {
        return typingSession;
    }

    public void setTypingSession(TypingSession typingSession) {
        this.typingSession = typingSession;
    }

    public WordEntry getWordEntry() {
        return wordEntry;
    }

    public void setWordEntry(WordEntry wordEntry) {
        this.wordEntry = wordEntry;
    }

    public int getAttemptsRequired() {
        return attemptsRequired;
    }

    public void setAttemptsRequired(int attemptsRequired) {
        this.attemptsRequired = attemptsRequired;
    }
}
