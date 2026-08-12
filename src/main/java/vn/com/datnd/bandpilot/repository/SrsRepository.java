package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.com.datnd.bandpilot.entity.SrsRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SrsRepository extends JpaRepository<SrsRecord, UUID> {

    List<SrsRecord> findByNextReviewDateLessThanEqual(LocalDate date);

    long countByNextReviewDateLessThanEqual(LocalDate date);

    long countByRepetitionsGreaterThanEqual(int repetitions);

    long countByIntervalGreaterThanEqual(int interval);

    /**
     * Inserts an SRS record using native SQL — bypasses Hibernate @MapsId
     * null-identifier issue when called within the same transaction as WordEntry creation.
     */
    @Modifying
    @Query(value = "INSERT INTO srs_record (word_id, interval, ease_factor, repetitions, next_review_date) " +
                   "VALUES (:wordId, 1, 2.50, 0, :nextReviewDate) ON CONFLICT (word_id) DO NOTHING",
           nativeQuery = true)
    void insertIfAbsent(@Param("wordId") UUID wordId,
                        @Param("nextReviewDate") LocalDate nextReviewDate);
}
