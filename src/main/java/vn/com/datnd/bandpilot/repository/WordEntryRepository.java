package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.WordEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WordEntry}.
 */
@Repository
public interface WordEntryRepository extends JpaRepository<WordEntry, UUID> {

    /**
     * Case-insensitive exact lookup by word — used for duplicate detection on create.
     */
    Optional<WordEntry> findByWordIgnoreCase(String word);

    /**
     * Checks whether a word exists (case-insensitive) — used for duplicate detection on create.
     */
    boolean existsByWordIgnoreCase(String word);

    /**
     * Checks for a case-insensitive duplicate word excluding the entry with the given id —
     * used for duplicate detection on update.
     */
    boolean existsByWordIgnoreCaseAndIdNot(String word, UUID id);

    /**
     * Batch case-insensitive lookup — used by SmartImportService to identify
     * which candidate words already exist in the vocabulary.
     */
    List<WordEntry> findAllByWordIgnoreCaseIn(Collection<String> words);

    /**
     * Filters entries by status ("New", "Learning", "Known").
     */
    List<WordEntry> findByStatus(String status);

    /**
     * Full-text search across {@code word} and {@code meaning} fields (case-insensitive substring match).
     */
    List<WordEntry> findByWordContainingIgnoreCaseOrMeaningContainingIgnoreCase(String word, String meaning);
}
