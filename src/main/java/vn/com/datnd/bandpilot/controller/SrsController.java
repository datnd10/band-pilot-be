package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.com.datnd.bandpilot.dto.DueWordResponse;
import vn.com.datnd.bandpilot.dto.ProgressResponse;
import vn.com.datnd.bandpilot.dto.ReviewRequest;
import vn.com.datnd.bandpilot.dto.ReviewResponse;
import vn.com.datnd.bandpilot.service.SrsService;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Spaced Repetition System (SRS) operations.
 *
 * <p>All endpoints are under {@code /api/v1/srs} and are JWT-protected by
 * the existing {@link vn.com.datnd.bandpilot.config.SecurityConfig}.
 * Error handling (404, 400) is delegated to
 * {@link vn.com.datnd.bandpilot.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/srs")
public class SrsController {

    private final SrsService srsService;

    public SrsController(SrsService srsService) {
        this.srsService = srsService;
    }

    /**
     * GET /api/v1/srs/due
     * Returns all words whose next review date is on or before today (Vietnam time).
     *
     * @return 200 OK with list of due word responses
     */
    @GetMapping("/due")
    public ResponseEntity<List<DueWordResponse>> getDueWords() {
        return ResponseEntity.ok(srsService.getDueWords());
    }

    /**
     * POST /api/v1/srs/review
     * Applies the SM-2 algorithm for the given word and rating, persisting the result.
     *
     * @param request validated review request containing wordId and rating
     * @return 200 OK with updated SRS scheduling data
     */
    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = srsService.applyReview(request.getWordId(), request.getRating());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/srs/due-count
     * Returns the count of words due for review today (Vietnam time).
     *
     * @return 200 OK with {@code {"count": <n>}}
     */
    @GetMapping("/due-count")
    public ResponseEntity<Map<String, Long>> getDueCount() {
        long count = srsService.getDueCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * GET /api/v1/srs/progress
     * Returns aggregate SRS progress statistics.
     *
     * @return 200 OK with progress statistics
     */
    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgress() {
        return ResponseEntity.ok(srsService.getProgress());
    }
}
