package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * An example sentence illustrating the usage of a {@link WordEntry}.
 * At most 3 examples per word are allowed; enforcement is at the service layer.
 */
@Entity
@Table(name = "word_example")
public class WordExample {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntry wordEntry;

    @Column(name = "sentence", nullable = false, length = 500)
    private String sentence;

    /** Display order within the word's example list (1, 2, or 3). */
    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected WordExample() {
    }

    public WordExample(WordEntry wordEntry, String sentence, short sortOrder) {
        this.wordEntry = wordEntry;
        this.sentence = sentence;
        this.sortOrder = sortOrder;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public WordEntry getWordEntry() {
        return wordEntry;
    }

    public void setWordEntry(WordEntry wordEntry) {
        this.wordEntry = wordEntry;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(short sortOrder) {
        this.sortOrder = sortOrder;
    }
}
