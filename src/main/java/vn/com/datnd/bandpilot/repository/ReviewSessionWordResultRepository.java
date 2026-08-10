package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.ReviewSession;
import vn.com.datnd.bandpilot.entity.ReviewSessionWordResult;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ReviewSessionWordResult}.
 *
 * <p>The parent session is mapped via the {@code reviewSession} object reference,
 * so queries navigate through that association rather than a raw {@code sessionId} column.</p>
 */
@Repository
public interface ReviewSessionWordResultRepository extends JpaRepository<ReviewSessionWordResult, UUID> {

    /**
     * Returns all word results for the given review session.
     */
    List<ReviewSessionWordResult> findByReviewSession(ReviewSession reviewSession);
}
