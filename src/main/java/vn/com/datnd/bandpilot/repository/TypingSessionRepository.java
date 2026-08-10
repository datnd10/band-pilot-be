package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.TypingSession;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TypingSession}.
 */
@Repository
public interface TypingSessionRepository extends JpaRepository<TypingSession, UUID> {
}
