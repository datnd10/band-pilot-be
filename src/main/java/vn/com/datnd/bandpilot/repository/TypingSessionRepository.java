package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.TypingSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TypingSession}.
 */
@Repository
public interface TypingSessionRepository extends JpaRepository<TypingSession, UUID> {

    /**
     * Returns all completedAt timestamps for a user — used for streak calculation.
     */
    @Query("SELECT ts.completedAt FROM TypingSession ts WHERE ts.userId = :userId")
    List<Instant> findAllCompletedAtByUserId(@Param("userId") UUID userId);
}
