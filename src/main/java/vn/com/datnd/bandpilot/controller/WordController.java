package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.com.datnd.bandpilot.dto.WordRequest;
import vn.com.datnd.bandpilot.dto.WordResponse;
import vn.com.datnd.bandpilot.service.WordService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for vocabulary word CRUD operations.
 *
 * <p>All error handling (400, 404, 409) is delegated to {@link vn.com.datnd.bandpilot.exception.GlobalExceptionHandler}.
 * This controller is intentionally thin — it only maps HTTP requests to service calls.
 */
@RestController
@RequestMapping("/api/v1/words")
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    /**
     * GET /api/v1/words
     * Returns all words, with optional filtering by search text, type, and status.
     *
     * @param search optional case-insensitive substring matched against word or meaning
     * @param type   optional exact match on word type (noun, verb, etc.)
     * @param status optional exact match on word status
     * @return 200 OK with list of matching words
     */
    @GetMapping
    public ResponseEntity<List<WordResponse>> getAllWords(
            @RequestParam Optional<String> search,
            @RequestParam Optional<String> type,
            @RequestParam Optional<String> status) {

        List<WordResponse> words = wordService.getAllWords(
                search.orElse(null),
                type.orElse(null),
                status.orElse(null)
        );
        return ResponseEntity.ok(words);
    }

    /**
     * POST /api/v1/words
     * Creates a new word entry.
     *
     * @param request validated word data
     * @return 201 Created with the new word
     */
    @PostMapping
    public ResponseEntity<WordResponse> createWord(@Valid @RequestBody WordRequest request) {
        WordResponse created = wordService.createWord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/words/{id}
     * Retrieves a single word by its UUID.
     *
     * @param id the word's UUID
     * @return 200 OK with the word, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<WordResponse> getWordById(@PathVariable UUID id) {
        WordResponse word = wordService.getWordById(id);
        return ResponseEntity.ok(word);
    }

    /**
     * PUT /api/v1/words/{id}
     * Updates an existing word entry.
     *
     * @param id      the word's UUID
     * @param request validated updated word data
     * @return 200 OK with the updated word, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<WordResponse> updateWord(
            @PathVariable UUID id,
            @Valid @RequestBody WordRequest request) {

        WordResponse updated = wordService.updateWord(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/words/{id}
     * Deletes a word entry and all its associated examples.
     *
     * @param id the word's UUID
     * @return 204 No Content, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWord(@PathVariable UUID id) {
        wordService.deleteWord(id);
        return ResponseEntity.noContent().build();
    }
}
