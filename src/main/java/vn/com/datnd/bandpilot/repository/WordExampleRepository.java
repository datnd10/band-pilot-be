package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.entity.WordExample;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WordExample}.
 *
 * <p>Note: {@code WordExample} maps the parent relationship via the {@code wordEntry} field
 * (a {@link WordEntry} object reference), so query methods use {@code WordEntry} as the
 * navigation path rather than a raw {@code wordId} UUID column.</p>
 */
@Repository
public interface WordExampleRepository extends JpaRepository<WordExample, UUID> {

    /**
     * Fetches all examples for the given word, ordered by {@code sortOrder} ascending.
     */
    List<WordExample> findByWordEntryOrderBySortOrderAsc(WordEntry wordEntry);

    /**
     * Counts the number of examples for the given word — used for the 3-example limit check.
     */
    int countByWordEntry(WordEntry wordEntry);

    /**
     * Removes all examples belonging to the given word.
     */
    @Transactional
    void deleteByWordEntry(WordEntry wordEntry);
}
