package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a single English vocabulary word with its metadata.
 *
 * <p>The {@code word} column has a standard JPA unique constraint for H2/dev compatibility.
 * For PostgreSQL production, add a case-insensitive expression index:
 * {@code CREATE UNIQUE INDEX uq_word_entry_word_lower ON word_entry (LOWER(word));}
 * and drop the plain unique constraint if preferred.
 * </p>
 */
@Entity
@Table(
        name = "word_entry",
        uniqueConstraints = @UniqueConstraint(name = "uq_word_entry_word", columnNames = "word")
)
public class WordEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The English word. Case-insensitive uniqueness is enforced at the service layer;
     * the DB constraint here is a safety net.
     * Production PostgreSQL should use: LOWER(word) expression index.
     */
    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Column(name = "phonetic", length = 200)
    private String phonetic;

    /** Allowed values: noun, verb, adjective, adverb, phrase. Validated at service layer. */
    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "meaning", nullable = false, length = 500)
    private String meaning;

    /** Allowed values: New, Learning, Known. Defaults to "New". */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "New";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "wordEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    private List<WordExample> examples = new ArrayList<>();

    @OneToMany(
            mappedBy = "wordEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<GroupWordMembership> groupMemberships = new HashSet<>();

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected WordEntry() {
    }

    public WordEntry(String word, String meaning) {
        this.word = word;
        this.meaning = meaning;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<WordExample> getExamples() {
        return examples;
    }

    public Set<GroupWordMembership> getGroupMemberships() {
        return groupMemberships;
    }
}
