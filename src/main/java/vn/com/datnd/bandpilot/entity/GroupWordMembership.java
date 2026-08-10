package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;

/**
 * Join entity representing the many-to-many relationship between
 * {@link VocabularyGroup} and {@link WordEntry}.
 *
 * <p>Composite PK: {@code (group_id, word_id)} — managed via {@link GroupWordMembershipId}.</p>
 */
@Entity
@Table(name = "group_word_membership")
@IdClass(GroupWordMembershipId.class)
public class GroupWordMembership {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private VocabularyGroup vocabularyGroup;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordEntry wordEntry;

    // ── Constructors ──────────────────────────────────────────────────────────────

    protected GroupWordMembership() {
    }

    public GroupWordMembership(VocabularyGroup vocabularyGroup, WordEntry wordEntry) {
        this.vocabularyGroup = vocabularyGroup;
        this.wordEntry = wordEntry;
    }

    // ── Getters ───────────────────────────────────────────────────────────────────

    public VocabularyGroup getVocabularyGroup() {
        return vocabularyGroup;
    }

    public WordEntry getWordEntry() {
        return wordEntry;
    }
}
