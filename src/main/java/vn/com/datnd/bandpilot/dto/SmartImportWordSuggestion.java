package vn.com.datnd.bandpilot.dto;

/**
 * A single word suggestion returned by the Smart Import analysis endpoint.
 *
 * <p>Contains dictionary data (phonetic, type, definition, example) and an
 * {@code alreadyExists} flag indicating whether the word is already in the
 * user's vocabulary. The {@code meaning} field is always returned as an empty
 * string — the user fills in the Vietnamese meaning before import.
 */
public class SmartImportWordSuggestion {

    private String word;
    private String phonetic;   // nullable — IPA notation
    private String type;       // nullable: noun / verb / adjective / adverb
    private String meaning;    // always "" — user fills in after selection
    private String definition; // English definition from Free Dictionary API
    private String example;    // example sentence from API (nullable)
    private boolean alreadyExists; // true if word already exists in DB
    private Double frequency;  // Datamuse frequency per million words (lower = rarer = more advanced)

    public SmartImportWordSuggestion() {
    }

    public SmartImportWordSuggestion(String word, String phonetic, String type,
                                     String definition, String example, boolean alreadyExists) {
        this.word = word;
        this.phonetic = phonetic;
        this.type = type;
        this.meaning = "";
        this.definition = definition;
        this.example = example;
        this.alreadyExists = alreadyExists;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getDefinition() { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }

    public String getExample() { return example; }
    public void setExample(String example) { this.example = example; }

    public boolean isAlreadyExists() { return alreadyExists; }
    public void setAlreadyExists(boolean alreadyExists) { this.alreadyExists = alreadyExists; }

    public Double getFrequency() { return frequency; }
    public void setFrequency(Double frequency) { this.frequency = frequency; }
}
