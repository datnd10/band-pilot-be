package vn.com.datnd.bandpilot.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key for {@link GroupWordMembership}.
 */
public class GroupWordMembershipId implements Serializable {

    private UUID vocabularyGroup;
    private UUID wordEntry;

    protected GroupWordMembershipId() {
    }

    public GroupWordMembershipId(UUID vocabularyGroup, UUID wordEntry) {
        this.vocabularyGroup = vocabularyGroup;
        this.wordEntry = wordEntry;
    }

    public UUID getVocabularyGroup() {
        return vocabularyGroup;
    }

    public UUID getWordEntry() {
        return wordEntry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupWordMembershipId that)) return false;
        return Objects.equals(vocabularyGroup, that.vocabularyGroup)
                && Objects.equals(wordEntry, that.wordEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vocabularyGroup, wordEntry);
    }
}
