package vn.com.datnd.bandpilot.dto;

/**
 * Request body for creating or renaming a vocabulary group.
 */
public class GroupRequest {

    private String name;

    // ── Constructors ──────────────────────────────────────────────────────────────

    public GroupRequest() {
    }

    public GroupRequest(String name) {
        this.name = name;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
