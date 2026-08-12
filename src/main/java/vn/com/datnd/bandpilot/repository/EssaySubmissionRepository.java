package vn.com.datnd.bandpilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.com.datnd.bandpilot.entity.EssaySubmission;

import java.util.List;
import java.util.UUID;

public interface EssaySubmissionRepository extends JpaRepository<EssaySubmission, UUID> {

    /**
     * Returns all essay submissions for the given user, newest first.
     *
     * @param userId the user whose submissions to retrieve
     * @return list of essay submissions ordered by submittedAt descending
     */
    List<EssaySubmission> findByUserIdOrderBySubmittedAtDesc(UUID userId);
}
