package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.entity.GroupWordMembership;
import vn.com.datnd.bandpilot.entity.GroupWordMembershipId;
import vn.com.datnd.bandpilot.entity.VocabularyGroup;
import vn.com.datnd.bandpilot.entity.WordEntry;

import java.util.List;

/**
 * Spring Data JPA repository for {@link GroupWordMembership}.
 *
 * <p>The composite PK is {@link GroupWordMembershipId}. The entity fields are
 * {@code vocabularyGroup} and {@code wordEntry} object references, so all derived
 * query methods navigate through those associations.</p>
 */
@Repository
public interface GroupWordMembershipRepository extends JpaRepository<GroupWordMembership, GroupWordMembershipId> {

    /**
     * Returns all memberships for the given group.
     */
    List<GroupWordMembership> findByVocabularyGroup(VocabularyGroup vocabularyGroup);

    /**
     * Checks whether a specific word is a member of the given group.
     */
    boolean existsByVocabularyGroupAndWordEntry(VocabularyGroup vocabularyGroup, WordEntry wordEntry);

    /**
     * Removes all memberships for the given group — called when deleting a group.
     */
    @Transactional
    void deleteByVocabularyGroup(VocabularyGroup vocabularyGroup);

    /**
     * Removes a specific word from a group.
     */
    @Transactional
    void deleteByVocabularyGroupAndWordEntry(VocabularyGroup vocabularyGroup, WordEntry wordEntry);
}
