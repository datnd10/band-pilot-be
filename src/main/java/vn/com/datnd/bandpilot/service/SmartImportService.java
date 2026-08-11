package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.com.datnd.bandpilot.dto.SmartImportWordSuggestion;
import vn.com.datnd.bandpilot.exception.ValidationException;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Analyses a passage of English text and returns vocabulary suggestions.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Validate text length ≤ 5000</li>
 *   <li>Tokenize, lowercase, deduplicate</li>
 *   <li>Filter stop words, short tokens, non-alphabetic</li>
 *   <li>Batch-check DB for already-existing words</li>
 *   <li>Fetch dictionary data in parallel (max 5 concurrent, 5 s timeout each)</li>
 *   <li>Sort: new words first, then by word length descending</li>
 *   <li>Return at most 30 suggestions</li>
 * </ol>
 */
@Service
public class SmartImportService {

    private static final Logger log = LoggerFactory.getLogger(SmartImportService.class);

    // ── Constants ─────────────────────────────────────────────────────────────────

    private static final int MAX_TEXT_LENGTH = 5000;
    private static final int MIN_WORD_LENGTH = 4;
    private static final int MAX_CANDIDATES   = 80;  // more candidates to feed into freq filter
    private static final int MAX_SUGGESTIONS  = 30;
    private static final int MAX_CONCURRENT   = 5;
    private static final int TIMEOUT_SECONDS  = 5;
    /** Words with Datamuse frequency above this threshold are considered too common */
    private static final double MAX_FREQUENCY = 10.0;

    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "up", "about", "into", "through", "during",
        "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
        "do", "does", "did", "will", "would", "could", "should", "may", "might",
        "shall", "can", "need", "dare", "ought", "used", "not", "no", "nor",
        "so", "yet", "both", "either", "neither", "each", "every", "all", "any",
        "few", "more", "most", "other", "some", "such", "than", "too", "very",
        "just", "because", "as", "until", "while", "although", "though", "if",
        "when", "where", "who", "which", "that", "this", "these", "those", "it",
        "its", "i", "me", "my", "we", "our", "you", "your", "he", "him", "his",
        "she", "her", "they", "them", "their", "what", "how", "also", "then",
        "there", "here", "now", "only", "even", "after", "before", "between",
        "same", "over", "under", "again", "further", "once", "well",
        "like", "get", "got", "make", "made", "take", "taken", "come", "came",
        "know", "knew", "think", "thought", "look", "want", "give", "given",
        "back", "down", "much", "time", "year", "good", "long",
        "little", "right", "still", "own", "old", "however", "without", "around"
    );

    private final WordEntryRepository wordEntryRepository;
    private final RestTemplate restTemplate;

    public SmartImportService(WordEntryRepository wordEntryRepository) {
        this.wordEntryRepository = wordEntryRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_SECONDS * 1000);
        factory.setReadTimeout(TIMEOUT_SECONDS * 1000);

        this.restTemplate = new RestTemplate(factory);
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Analyses the supplied text and returns word suggestions enriched with
     * dictionary data.
     *
     * @param text the English passage to analyse (max 5000 chars)
     * @return list of up to 30 suggestions, sorted new-words-first
     * @throws ValidationException if text exceeds the maximum length
     */
    public List<SmartImportWordSuggestion> analyzeText(String text) {
        if (text == null || text.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException(
                "Text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }

        // 1. Tokenize
        Set<String> tokens = tokenize(text);

        // 2. Filter candidates
        List<String> candidates = tokens.stream()
            .filter(w -> w.length() >= MIN_WORD_LENGTH)
            .filter(w -> !STOP_WORDS.contains(w))
            .filter(w -> w.matches("[a-z]+"))
            .limit(MAX_CANDIDATES)
            .collect(Collectors.toList());

        log.debug("Smart import: {} tokens → {} candidates", tokens.size(), candidates.size());

        // 3. Identify existing words (batch DB query)
        Set<String> existingWords = wordEntryRepository
            .findAllByWordIgnoreCaseIn(new HashSet<>(candidates))
            .stream()
            .map(w -> w.getWord().toLowerCase())
            .collect(Collectors.toSet());

        // 4. Filter by Datamuse frequency — keep only rare/advanced words (freq < 10)
        //    Words already in DB skip frequency filter (we still want to show them as existing)
        List<String> filteredCandidates = filterByFrequency(candidates, existingWords);
        log.debug("Smart import: {} candidates → {} after frequency filter",
            candidates.size(), filteredCandidates.size());

        // 5. Fetch dictionary info in parallel for filtered candidates
        List<SmartImportWordSuggestion> suggestions =
            fetchDictionaryInfoParallel(filteredCandidates, existingWords);

        // 6. Sort: non-existing first, then rarest first (lowest frequency = most advanced)
        suggestions.sort(Comparator
            .comparing(SmartImportWordSuggestion::isAlreadyExists)
            .thenComparingDouble(s -> s.getFrequency() == null ? 999.0 : s.getFrequency()));

        return suggestions.stream()
            .limit(MAX_SUGGESTIONS)
            .collect(Collectors.toList());
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[^a-z]+"))
            .filter(w -> !w.isEmpty())
            .collect(Collectors.toSet());
    }

    private List<SmartImportWordSuggestion> fetchDictionaryInfoParallel(
            List<String> words, Set<String> existingWords) {

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        List<Future<SmartImportWordSuggestion>> futures = words.stream()
            .map(word -> executor.submit(
                () -> fetchWordInfo(word, existingWords.contains(word.toLowerCase()))))
            .collect(Collectors.toList());

        executor.shutdown();

        List<SmartImportWordSuggestion> result = new ArrayList<>();
        for (Future<SmartImportWordSuggestion> future : futures) {
            try {
                SmartImportWordSuggestion suggestion =
                    future.get(TIMEOUT_SECONDS + 1L, TimeUnit.SECONDS);
                if (suggestion != null) {
                    result.add(suggestion);
                }
            } catch (TimeoutException e) {
                log.debug("Dictionary fetch timed out for a candidate word");
                future.cancel(true);
            } catch (Exception e) {
                log.debug("Dictionary fetch failed: {}", e.getMessage());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private SmartImportWordSuggestion fetchWordInfo(String word, boolean alreadyExists) {
        try {
            String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
            ResponseEntity<List> response =
                restTemplate.exchange(url, HttpMethod.GET, null, List.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && !response.getBody().isEmpty()) {

                Map<String, Object> entry =
                    (Map<String, Object>) response.getBody().get(0);

                String phonetic   = extractPhonetic(entry);
                String type       = extractType(entry);
                String definition = extractDefinition(entry);
                String example    = extractExample(entry);

                SmartImportWordSuggestion suggestion = new SmartImportWordSuggestion();
                suggestion.setWord(word);
                suggestion.setPhonetic(phonetic);
                suggestion.setType(normalizeType(type));
                suggestion.setMeaning("");
                suggestion.setDefinition(definition);
                suggestion.setExample(example);
                suggestion.setAlreadyExists(alreadyExists);
                // Frequency will be set later if needed; leave null for now
                return suggestion;
            }
        } catch (Exception e) {
            // 404 (word not found) or network/timeout — silently skip
            log.trace("Skipping '{}': {}", word, e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractPhonetic(Map<String, Object> entry) {
        Object phonetic = entry.get("phonetic");
        if (phonetic instanceof String s && !s.isEmpty()) {
            return s;
        }
        List<Map<String, Object>> phonetics =
            (List<Map<String, Object>>) entry.get("phonetics");
        if (phonetics != null) {
            return phonetics.stream()
                .filter(p -> p.get("text") instanceof String t && !t.isEmpty())
                .map(p -> (String) p.get("text"))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractType(Map<String, Object> entry) {
        List<Map<String, Object>> meanings =
            (List<Map<String, Object>>) entry.get("meanings");
        if (meanings != null && !meanings.isEmpty()) {
            Object pos = meanings.get(0).get("partOfSpeech");
            return pos instanceof String s ? s : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractDefinition(Map<String, Object> entry) {
        List<Map<String, Object>> meanings =
            (List<Map<String, Object>>) entry.get("meanings");
        if (meanings != null && !meanings.isEmpty()) {
            List<Map<String, Object>> defs =
                (List<Map<String, Object>>) meanings.get(0).get("definitions");
            if (defs != null && !defs.isEmpty()) {
                Object def = defs.get(0).get("definition");
                return def instanceof String s ? s : null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractExample(Map<String, Object> entry) {
        List<Map<String, Object>> meanings =
            (List<Map<String, Object>>) entry.get("meanings");
        if (meanings != null) {
            for (Map<String, Object> meaning : meanings) {
                List<Map<String, Object>> defs =
                    (List<Map<String, Object>>) meaning.get("definitions");
                if (defs != null) {
                    for (Map<String, Object> def : defs) {
                        Object ex = def.get("example");
                        if (ex instanceof String s && !s.isEmpty()) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    private String normalizeType(String partOfSpeech) {
        if (partOfSpeech == null) return null;
        return switch (partOfSpeech.toLowerCase()) {
            case "noun"      -> "noun";
            case "verb"      -> "verb";
            case "adjective" -> "adjective";
            case "adverb"    -> "adverb";
            default          -> null;
        };
    }

    // ── Datamuse frequency filter ─────────────────────────────────────────────────

    /**
     * Filters candidates by Datamuse frequency score.
     * Words with frequency ≥ MAX_FREQUENCY are considered too common and excluded.
     * Words already in the user's DB bypass the filter (shown as alreadyExists=true).
     *
     * @param candidates   all tokenized candidates
     * @param existingWords words already in DB (bypass frequency filter)
     * @return candidates that are rare enough to be worth learning
     */
    private List<String> filterByFrequency(List<String> candidates, Set<String> existingWords) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        List<Future<Map.Entry<String, Double>>> futures = candidates.stream()
            .map(word -> executor.submit(() -> fetchFrequency(word)))
            .collect(Collectors.toList());
        executor.shutdown();

        List<String> result = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            String word = candidates.get(i);
            // Words already in DB always pass through
            if (existingWords.contains(word)) {
                result.add(word);
                continue;
            }
            try {
                Map.Entry<String, Double> entry = futures.get(i).get(TIMEOUT_SECONDS + 1L, TimeUnit.SECONDS);
                if (entry != null && entry.getValue() < MAX_FREQUENCY) {
                    result.add(word);
                    log.debug("Keeping '{}' (freq={})", word, entry.getValue());
                } else if (entry != null) {
                    log.debug("Filtering '{}' — too common (freq={})", word, entry.getValue());
                }
                // null = Datamuse didn't find it → skip (not a real English word)
            } catch (TimeoutException e) {
                // On timeout, include word anyway (don't penalise for slow API)
                result.add(word);
                futures.get(i).cancel(true);
            } catch (Exception e) {
                result.add(word); // On error, include conservatively
            }
        }
        return result;
    }

    /**
     * Fetches the Datamuse frequency for a single word.
     *
     * @return a Map.Entry of (word, frequency) or null if word not found
     */
    @SuppressWarnings("unchecked")
    private Map.Entry<String, Double> fetchFrequency(String word) {
        try {
            String url = "https://api.datamuse.com/words?sp=" + word + "&md=f&max=1";
            ResponseEntity<List> response =
                restTemplate.exchange(url, HttpMethod.GET, null, List.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && !response.getBody().isEmpty()) {

                Map<String, Object> entry = (Map<String, Object>) response.getBody().get(0);

                // Verify the returned word matches exactly (Datamuse may return closest match)
                String returnedWord = (String) entry.get("word");
                if (!word.equals(returnedWord)) {
                    return null; // different word returned — skip
                }

                List<String> tags = (List<String>) entry.get("tags");
                if (tags != null) {
                    for (String tag : tags) {
                        if (tag.startsWith("f:")) {
                            double freq = Double.parseDouble(tag.substring(2));
                            return Map.entry(word, freq);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.trace("Datamuse freq fetch failed for '{}': {}", word, e.getMessage());
        }
        return null;
    }
}
