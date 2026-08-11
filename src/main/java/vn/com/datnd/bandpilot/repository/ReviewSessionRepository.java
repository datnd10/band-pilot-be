package vn.com.datnd.bandpilot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.ReviewSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ReviewSession}.
 */
@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, UUID> {

    /**
     * Returns a page of sessions for the given user, ordered by completedAt descending.
     */
    Page<ReviewSession> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);

    /**
     * Fetches a session with its wordResults eagerly loaded (avoids N+1 in detail view).
     */
    @Query("SELECT rs FROM ReviewSession rs LEFT JOIN FETCH rs.wordResults wr LEFT JOIN FETCH wr.wordEntry WHERE rs.id = :id")
    java.util.Optional<ReviewSession> findByIdWithWordResults(@Param("id") UUID id);

    /**
     * Returns all completedAt timestamps for a user — used for streak calculation.
     */
    @Query("SELECT rs.completedAt FROM ReviewSession rs WHERE rs.userId = :userId")
    List<Instant> findAllCompletedAtByUserId(@Param("userId") UUID userId);
}
