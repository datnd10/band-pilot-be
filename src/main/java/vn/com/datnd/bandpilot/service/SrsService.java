package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.DueWordResponse;
import vn.com.datnd.bandpilot.dto.ProgressResponse;
import vn.com.datnd.bandpilot.dto.Rating;
import vn.com.datnd.bandpilot.dto.ReviewResponse;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.entity.SrsRecord;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.entity.WordExample;
import vn.com.datnd.bandpilot.repository.SrsRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;
import vn.com.datnd.bandpilot.repository.WordExampleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for the Spaced Repetition System (SRS).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Creating SRS records for new words (idempotent)</li>
 *   <li>Querying due words and counts (next_review_date &le; today, Vietnam time)</li>
 *   <li>Applying the SM-2 algorithm when a user submits a review rating</li>
 *   <li>Providing aggregate progress statistics</li>
 * </ul>
 * </p>
 */
@Service
public class SrsService {

    private static final Logger log = LoggerFactory.getLogger(SrsService.class);

    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final BigDecimal MIN_EF = new BigDecimal("1.30");
    static final BigDecimal EF_DEFAULT = new BigDecimal("2.50");
    static final BigDecimal EF_EASY_BONUS = new BigDecimal("0.15");
    static final BigDecimal EF_AGAIN_PENALTY = new BigDecimal("0.20");
    static final BigDecimal EASY_INTERVAL_MULTIPLIER = new BigDecimal("1.3");

    private final SrsRepository srsRepository;
    private final WordEntryRepository wordEntryRepository;
    private final WordExampleRepository wordExampleRepository;

    public SrsService(SrsRepository srsRepository,
                      WordEntryRepository wordEntryRepository,
                      WordExampleRepository wordExampleRepository) {
        this.srsRepository = srsRepository;
        this.wordEntryRepository = wordEntryRepository;
        this.wordExampleRepository = wordExampleRepository;
    }

    // ── Initialization ────────────────────────────────────────────────────────

    /**
     * Creates an {@link SrsRecord} for the given {@link WordEntry} with default values
     * if one does not already exist. Idempotent — safe to call multiple times.
     *
     * <p>Default values: {@code interval=1}, {@code easeFactor=2.50},
     * {@code repetitions=0}, {@code nextReviewDate=today(VN)}.</p>
     *
     * @param wordEntry the word entry to initialise SRS scheduling for
     */
    @Transactional
    public void initializeIfAbsent(WordEntry wordEntry) {
        UUID wordId = wordEntry.getId();
        if (srsRepository.existsById(wordId)) {
            log.debug("SRS record already exists for word id={}, skipping initialisation", wordId);
            return;
        }

        LocalDate today = LocalDate.now(VN_ZONE);
        SrsRecord record = new SrsRecord(
                wordId,
                wordEntry,
                1,
                EF_DEFAULT,
                0,
                today
        );
        srsRepository.save(record);
        log.info("SRS record created: wordId={} nextReviewDate={}", wordId, today);
    }

    // ── Due words ─────────────────────────────────────────────────────────────

    /**
     * Returns all {@link DueWordResponse} objects for words whose
     * {@code next_review_date} is on or before today (Vietnam date).
     *
     * <p>Each response includes nullable {@code phonetic}, {@code type}, and the
     * first example sentence (nullable).</p>
     *
     * @return list of due word responses; empty list if no words are due
     */
    @Transactional(readOnly = true)
    public List<DueWordResponse> getDueWords() {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<SrsRecord> dueRecords = srsRepository.findByNextReviewDateLessThanEqual(today);

        return dueRecords.stream()
                .map(record -> {
                    WordEntry word = record.getWordEntry();
                    List<WordExample> allExamples =
                            wordExampleRepository.findByWordEntryOrderBySortOrderAsc(word);
                    List<String> examplesList = allExamples.stream()
                            .map(WordExample::getSentence)
                            .collect(Collectors.toList());

                    DueWordResponse dueWordResponse = new DueWordResponse(
                            word.getId(),
                            word.getWord(),
                            word.getMeaning(),
                            word.getPhonetic(),   // nullable
                            word.getType(),        // nullable
                            examplesList.isEmpty() ? null : examplesList.get(0)  // backward-compat
                    );
                    dueWordResponse.setExamples(examplesList);
                    return dueWordResponse;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the count of words due for review on or before today (Vietnam date).
     * Used by the navigation badge and the daily email scheduler.
     *
     * @return number of due words
     */
    @Transactional(readOnly = true)
    public long getDueCount() {
        LocalDate today = LocalDate.now(VN_ZONE);
        return srsRepository.countByNextReviewDateLessThanEqual(today);
    }

    // ── Review ────────────────────────────────────────────────────────────────

    /**
     * Applies the SM-2 algorithm for the given rating and persists the updated record.
     *
     * @param wordId the UUID of the word being reviewed
     * @param rating the user's recall rating
     * @return updated SRS scheduling data
     * @throws ResourceNotFoundException if no SrsRecord exists for wordId
     */
    @Transactional
    public ReviewResponse applyReview(UUID wordId, Rating rating) {
        SrsRecord record = srsRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SrsRecord not found for wordId: " + wordId));

        LocalDate today = LocalDate.now(VN_ZONE);
        applySm2(record, rating, today);
        srsRepository.save(record);

        log.info("Review applied: wordId={} rating={} nextReviewDate={} interval={} repetitions={}",
                wordId, rating, record.getNextReviewDate(), record.getInterval(), record.getRepetitions());

        return new ReviewResponse(
                record.getWordId(),
                record.getNextReviewDate(),
                record.getInterval(),
                record.getEaseFactor(),
                record.getRepetitions()
        );
    }

    // ── Progress stats ────────────────────────────────────────────────────────

    /**
     * Returns aggregate SRS statistics: total words, due today, learned, and mature.
     *
     * <ul>
     *   <li>{@code totalWords} — count of all SRS records</li>
     *   <li>{@code dueToday} — count where {@code next_review_date <= today} (VN)</li>
     *   <li>{@code learnedWords} — count where {@code repetitions >= 1}</li>
     *   <li>{@code matureWords} — count where {@code interval >= 21}</li>
     * </ul>
     *
     * @return progress statistics
     */
    @Transactional(readOnly = true)
    public ProgressResponse getProgress() {
        LocalDate today = LocalDate.now(VN_ZONE);
        long totalWords   = srsRepository.count();
        long dueToday     = srsRepository.countByNextReviewDateLessThanEqual(today);
        long learnedWords = srsRepository.countByRepetitionsGreaterThanEqual(1);
        long matureWords  = srsRepository.countByIntervalGreaterThanEqual(21);

        return new ProgressResponse(totalWords, dueToday, learnedWords, matureWords);
    }

    // ── SM-2 algorithm (package-private — implemented in task 3.4) ───────────

    /**
     * Applies the SM-2 scheduling algorithm to the given record for the supplied
     * rating and reference date. Package-private so jqwik property tests can call
     * it directly without a Spring context.
     *
     * @param record today's SRS record state
     * @param rating the user's recall rating
     * @param today  the reference date for computing {@code nextReviewDate}
     * @return the mutated record (same instance, updated in place)
     */
    SrsRecord applySm2(SrsRecord record, Rating rating, LocalDate today) {
        int newInterval;
        int newRepetitions;
        BigDecimal newEaseFactor;

        switch (rating) {
            case AGAIN -> {
                newInterval = 1;
                newRepetitions = 0;
                newEaseFactor = record.getEaseFactor().subtract(EF_AGAIN_PENALTY).max(MIN_EF);
            }
            case GOOD -> {
                newInterval = BigDecimal.valueOf(record.getInterval())
                        .multiply(record.getEaseFactor())
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                newRepetitions = record.getRepetitions() + 1;
                newEaseFactor = record.getEaseFactor();
            }
            case EASY -> {
                newInterval = BigDecimal.valueOf(record.getInterval())
                        .multiply(record.getEaseFactor())
                        .multiply(EASY_INTERVAL_MULTIPLIER)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                newRepetitions = record.getRepetitions() + 1;
                newEaseFactor = record.getEaseFactor().add(EF_EASY_BONUS);
            }
            default -> throw new IllegalArgumentException("Unknown rating: " + rating);
        }

        // Enforce invariants
        newInterval = Math.max(newInterval, 1);
        newEaseFactor = newEaseFactor.max(MIN_EF);

        record.setInterval(newInterval);
        record.setRepetitions(newRepetitions);
        record.setEaseFactor(newEaseFactor);
        record.setNextReviewDate(today.plusDays(newInterval));

        return record;
    }
}
