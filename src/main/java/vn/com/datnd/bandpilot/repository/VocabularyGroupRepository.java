package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.VocabularyGroup;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link VocabularyGroup}.
 */
@Repository
public interface VocabularyGroupRepository extends JpaRepository<VocabularyGroup, UUID> {

    /**
     * Case-insensitive lookup by name — used for uniqueness check on create.
     */
    Optional<VocabularyGroup> findByNameIgnoreCase(String name);

    /**
     * Checks for a case-insensitive duplicate name excluding the group with the given id —
     * used for rename duplicate check.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
