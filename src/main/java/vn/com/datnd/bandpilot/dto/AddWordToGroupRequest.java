package vn.com.datnd.bandpilot.dto;

import java.util.UUID;

/**
 * Request body for adding an existing word to a vocabulary group.
 */
public class AddWordToGroupRequest {

    private UUID wordId;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public AddWordToGroupRequest() {
    }

    public AddWordToGroupRequest(UUID wordId) {
        this.wordId = wordId;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public UUID getWordId() {
        return wordId;
    }

    public void setWordId(UUID wordId) {
        this.wordId = wordId;
    }
}
