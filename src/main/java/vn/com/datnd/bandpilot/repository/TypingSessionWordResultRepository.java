package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.TypingSession;
import vn.com.datnd.bandpilot.entity.TypingSessionWordResult;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TypingSessionWordResult}.
 *
 * <p>The parent session is mapped via the {@code typingSession} object reference,
 * so queries navigate through that association rather than a raw {@code sessionId} column.</p>
 */
@Repository
public interface TypingSessionWordResultRepository extends JpaRepository<TypingSessionWordResult, UUID> {

    /**
     * Returns all word results for the given typing session.
     */
    List<TypingSessionWordResult> findByTypingSession(TypingSession typingSession);
}
