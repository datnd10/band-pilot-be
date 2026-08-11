package vn.com.datnd.bandpilot.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DueWordResponse {

    private UUID wordId;
    private String word;
    private String meaning;
    private String phonetic;
    private String type;
    private String example;
    private List<String> examples = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public DueWordResponse() {
    }

    public DueWordResponse(UUID wordId, String word, String meaning,
                           String phonetic, String type, String example) {
        this.wordId = wordId;
        this.word = word;
        this.meaning = meaning;
        this.phonetic = phonetic;
        this.type = type;
        this.example = example;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
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

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }
}
