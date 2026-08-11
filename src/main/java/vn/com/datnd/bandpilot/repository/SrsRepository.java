package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.com.datnd.bandpilot.entity.SrsRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SrsRepository extends JpaRepository<SrsRecord, UUID> {

    List<SrsRecord> findByNextReviewDateLessThanEqual(LocalDate date);

    long countByNextReviewDateLessThanEqual(LocalDate date);

    long countByRepetitionsGreaterThanEqual(int repetitions);

    long countByIntervalGreaterThanEqual(int interval);
}
