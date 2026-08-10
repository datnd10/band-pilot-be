package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body used for both POST (create) and PUT (update) word operations.
 */
public class WordRequest {

    @NotBlank(message = "English word is required")
    @Size(max = 100, message = "Word must not exceed 100 characters")
    private String word;

    @Size(max = 200, message = "Phonetic must not exceed 200 characters")
    private String phonetic;

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Meaning is required")
    @Size(max = 500, message = "Meaning must not exceed 500 characters")
    private String meaning;

    /** At most 3 items, each at most 500 characters. Validated at service layer. */
    private List<String> examples;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public WordRequest() {
    }

    public WordRequest(String word, String meaning) {
        this.word = word;
        this.meaning = meaning;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }
}
