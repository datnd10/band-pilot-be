package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.com.datnd.bandpilot.entity.ReviewSession;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ReviewSession}.
 */
@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, UUID> {
}
