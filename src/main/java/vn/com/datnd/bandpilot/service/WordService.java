package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.WordRequest;
import vn.com.datnd.bandpilot.dto.WordResponse;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.entity.WordExample;
import vn.com.datnd.bandpilot.exception.DuplicateResourceException;
import vn.com.datnd.bandpilot.exception.ExampleLimitExceededException;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.exception.ValidationException;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;
import vn.com.datnd.bandpilot.repository.WordExampleRepository;
import vn.com.datnd.bandpilot.repository.SrsRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for managing vocabulary word entries.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Field validation (word, meaning required; length limits; allowed type values)</li>
 *   <li>Case-insensitive duplicate detection</li>
 *   <li>Persisting {@link WordEntry} and up to 3 {@link WordExample} rows</li>
 *   <li>Filtering by search text, type, and status</li>
 * </ul>
 * </p>
 */
@Service
public class WordService {

    private static final Logger log = LoggerFactory.getLogger(WordService.class);

    private static final int MAX_EXAMPLES = 3;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "noun", "verb", "adjective", "adverb", "phrase"
    );

    private final WordEntryRepository wordEntryRepository;
    private final WordExampleRepository wordExampleRepository;
    private final SrsRepository srsRepository;

    public WordService(WordEntryRepository wordEntryRepository,
                       WordExampleRepository wordExampleRepository,
                       SrsRepository srsRepository) {
        this.wordEntryRepository = wordEntryRepository;
        this.wordExampleRepository = wordExampleRepository;
        this.srsRepository = srsRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────────

    /**
     * Creates a new word entry.
     *
     * @param request the word data
     * @return the saved word as a response DTO
     * @throws ValidationException          if required fields are missing or exceed length limits
     * @throws DuplicateResourceException   if a word with the same spelling (case-insensitive) already exists
     * @throws ExampleLimitExceededException if more than 3 examples are supplied
     */
    @Transactional
    public WordResponse createWord(WordRequest request) {
        validateWordRequest(request);

        if (wordEntryRepository.existsByWordIgnoreCase(request.getWord())) {
            log.warn("Duplicate word rejected: '{}'", request.getWord());
            throw new DuplicateResourceException(
                    "A word already exists with the spelling: " + request.getWord());
        }

        WordEntry entry = new WordEntry(request.getWord().trim(), request.getMeaning().trim());
        entry.setPhonetic(request.getPhonetic() != null ? request.getPhonetic().trim() : null);
        entry.setType(request.getType() != null ? request.getType().trim().toLowerCase() : null);

        WordEntry saved = wordEntryRepository.save(entry);
        saveExamples(request, saved);
        log.info("Word created: id={} word='{}'", saved.getId(), saved.getWord());
        return toResponse(saved);
    }

    // ── Read by ID ────────────────────────────────────────────────────────────────

    /**
     * Retrieves a single word by its UUID.
     *
     * @param id the word's UUID
     * @return the word as a response DTO
     * @throws ResourceNotFoundException if no word exists with that ID
     */
    @Transactional(readOnly = true)
    public WordResponse getWordById(UUID id) {
        WordEntry entry = findEntryOrThrow(id);
        WordResponse response = toResponse(entry);
        srsRepository.findById(entry.getId()).ifPresent(srs -> {
            response.setNextReviewDate(srs.getNextReviewDate());
            response.setInterval(srs.getInterval());
            response.setRepetitions(srs.getRepetitions());
        });
        return response;
    }

    // ── Read all (with optional filters) ─────────────────────────────────────────

    /**
     * Returns all words, optionally filtered by search text, type, and/or status.
     * Filters are combined with AND logic.
     *
     * @param search case-insensitive substring matched against {@code word} OR {@code meaning}
     * @param type   case-insensitive exact match against {@code type}
     * @param status case-insensitive exact match against {@code status}
     * @return list of matching words
     */
    @Transactional(readOnly = true)
    public List<WordResponse> getAllWords(String search, String type, String status) {
        List<WordEntry> all = wordEntryRepository.findAll();
        List<WordResponse> result = all.stream()
                .filter(e -> matchesSearch(e, search))
                .filter(e -> matchesType(e, type))
                .filter(e -> matchesStatus(e, status))
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.debug("getAllWords: search='{}' type='{}' status='{}' → {} results", search, type, status, result.size());
        return result;
    }

    // ── Update ────────────────────────────────────────────────────────────────────

    /**
     * Updates an existing word entry.
     *
     * @param id      the word's UUID
     * @param request the updated word data
     * @return the updated word as a response DTO
     * @throws ResourceNotFoundException     if no word exists with that ID
     * @throws ValidationException           if required fields are missing or exceed length limits
     * @throws DuplicateResourceException    if another word already uses the same spelling
     * @throws ExampleLimitExceededException if more than 3 examples are supplied
     */
    @Transactional
    public WordResponse updateWord(UUID id, WordRequest request) {
        WordEntry entry = findEntryOrThrow(id);

        validateWordRequest(request);

        if (wordEntryRepository.existsByWordIgnoreCaseAndIdNot(request.getWord(), id)) {
            throw new DuplicateResourceException(
                    "Another word already exists with the spelling: " + request.getWord());
        }

        entry.setWord(request.getWord().trim());
        entry.setMeaning(request.getMeaning().trim());
        entry.setPhonetic(request.getPhonetic() != null ? request.getPhonetic().trim() : null);
        entry.setType(request.getType() != null ? request.getType().trim().toLowerCase() : null);

        if (request.getExamples() != null) {
            if (request.getExamples().size() > MAX_EXAMPLES) {
                throw new ExampleLimitExceededException();
            }
            wordExampleRepository.deleteByWordEntry(entry);
            wordExampleRepository.flush();
            saveExamples(request, entry);
        }

        WordEntry saved = wordEntryRepository.save(entry);
        return toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────────

    /**
     * Deletes a word entry and all its associated examples.
     *
     * @param id the word's UUID
     * @throws ResourceNotFoundException if no word exists with that ID
     */
    @Transactional
    public void deleteWord(UUID id) {
        WordEntry entry = findEntryOrThrow(id);
        wordExampleRepository.deleteByWordEntry(entry);
        wordEntryRepository.delete(entry);
        log.info("Word deleted: id={} word='{}'", id, entry.getWord());
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Validates the required fields and constraints on a {@link WordRequest}.
     */
    private void validateWordRequest(WordRequest request) {
        if (request.getWord() == null || request.getWord().isBlank()) {
            throw new ValidationException("English word is required");
        }
        if (request.getWord().trim().length() > 100) {
            throw new ValidationException("Word must not exceed 100 characters");
        }
        if (request.getMeaning() == null || request.getMeaning().isBlank()) {
            throw new ValidationException("Meaning is required");
        }
        if (request.getMeaning().trim().length() > 500) {
            throw new ValidationException("Meaning must not exceed 500 characters");
        }
        if (request.getPhonetic() != null && request.getPhonetic().trim().length() > 200) {
            throw new ValidationException("Phonetic must not exceed 200 characters");
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            String normalizedType = request.getType().trim().toLowerCase();
            if (!ALLOWED_TYPES.contains(normalizedType)) {
                throw new ValidationException(
                        "Invalid word type '" + request.getType() + "'. Allowed values: noun, verb, adjective, adverb, phrase");
            }
        }
        if (request.getExamples() != null) {
            for (String example : request.getExamples()) {
                if (example != null && example.length() > 500) {
                    throw new ValidationException("Each example sentence must not exceed 500 characters");
                }
            }
        }
    }

    /**
     * Saves example sentences for a word entry (used in both create and update).
     * Assumes the examples list size has already been validated ≤ 3.
     */
    private void saveExamples(WordRequest request, WordEntry entry) {
        if (request.getExamples() == null || request.getExamples().isEmpty()) {
            return;
        }
        if (request.getExamples().size() > MAX_EXAMPLES) {
            throw new ExampleLimitExceededException();
        }
        List<String> examples = request.getExamples();
        for (int i = 0; i < examples.size(); i++) {
            String sentence = examples.get(i);
            if (sentence != null && !sentence.isBlank()) {
                WordExample ex = new WordExample(entry, sentence.trim(), (short) (i + 1));
                wordExampleRepository.save(ex);
            }
        }
    }

    /**
     * Finds a {@link WordEntry} by ID or throws {@link ResourceNotFoundException}.
     */
    private WordEntry findEntryOrThrow(UUID id) {
        return wordEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Word", id));
    }

    /**
     * Maps a {@link WordEntry} (and its examples) to a {@link WordResponse}.
     */
    private WordResponse toResponse(WordEntry entry) {
        WordResponse response = new WordResponse();
        response.setId(entry.getId());
        response.setWord(entry.getWord());
        response.setPhonetic(entry.getPhonetic());
        response.setType(entry.getType());
        response.setMeaning(entry.getMeaning());
        response.setStatus(entry.getStatus());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());

        List<String> sentences = wordExampleRepository
                .findByWordEntryOrderBySortOrderAsc(entry)
                .stream()
                .map(WordExample::getSentence)
                .collect(Collectors.toList());
        response.setExamples(sentences);

        return response;
    }

    // ── Filter predicates ─────────────────────────────────────────────────────────

    private boolean matchesSearch(WordEntry entry, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String lower = search.toLowerCase();
        return (entry.getWord() != null && entry.getWord().toLowerCase().contains(lower))
                || (entry.getMeaning() != null && entry.getMeaning().toLowerCase().contains(lower));
    }

    private boolean matchesType(WordEntry entry, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        return type.equalsIgnoreCase(entry.getType());
    }

    private boolean matchesStatus(WordEntry entry, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return status.equalsIgnoreCase(entry.getStatus());
    }
}
