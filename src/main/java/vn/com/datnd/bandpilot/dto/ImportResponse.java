package vn.com.datnd.bandpilot.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the CSV import operation.
 * Reports how many rows were imported, which group they were added to (if any),
 * and details about any skipped rows.
 */
public class ImportResponse {

    private int importedCount;
    private UUID groupId;
    private List<SkippedRow> skippedRows;

    // ── Nested class ──────────────────────────────────────────────────────────────

    /**
     * Describes a single row that was skipped during CSV import.
     */
    public static class SkippedRow {

        /** 1-based data row index (not counting the header row). */
        private int rowNumber;

        /** The value in the {@code word} column for this row (may be blank if missing). */
        private String word;

        /** Reason code: {@code "MISSING_REQUIRED_FIELD"} or {@code "DUPLICATE_WORD"}. */
        private String reason;

        public SkippedRow() {
        }

        public SkippedRow(int rowNumber, String word, String reason) {
            this.rowNumber = rowNumber;
            this.word = word;
            this.reason = reason;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    // ── Constructors ──────────────────────────────────────────────────────────────

    public ImportResponse() {
    }

    public ImportResponse(int importedCount, UUID groupId, List<SkippedRow> skippedRows) {
        this.importedCount = importedCount;
        this.groupId = groupId;
        this.skippedRows = skippedRows;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────────

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public List<SkippedRow> getSkippedRows() {
        return skippedRows;
    }

    public void setSkippedRows(List<SkippedRow> skippedRows) {
        this.skippedRows = skippedRows;
    }
}
