package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A named collection of vocabulary words.
 *
 * <p>The {@code name} column has a standard JPA unique constraint for H2/dev compatibility.
 * For PostgreSQL production, add a case-insensitive expression index:
 * {@code CREATE UNIQUE INDEX uq_vocabulary_group_name_lower ON vocabulary_group (LOWER(name));}
 * </p>
 */
@Entity
@Table(
        name = "vocabulary_group",
        uniqueConstraints = @UniqueConstraint(name = "uq_vocabulary_group_name", columnNames = "name")
)
public class VocabularyGroup {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "vocabularyGroup",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<GroupWordMembership> wordMemberships = new HashSet<>();

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected VocabularyGroup() {
    }

    public VocabularyGroup(String name) {
        this.name = name;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<GroupWordMembership> getWordMemberships() {
        return wordMemberships;
    }
}
