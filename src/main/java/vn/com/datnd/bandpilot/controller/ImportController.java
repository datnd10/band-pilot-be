package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.com.datnd.bandpilot.dto.ImportResponse;
import vn.com.datnd.bandpilot.dto.SmartImportRequest;
import vn.com.datnd.bandpilot.dto.SmartImportWordSuggestion;
import vn.com.datnd.bandpilot.service.CsvImportService;
import vn.com.datnd.bandpilot.service.SmartImportService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for bulk CSV vocabulary import.
 *
 * <p>All error handling (400, 404) is delegated to
 * {@link vn.com.datnd.bandpilot.exception.GlobalExceptionHandler}.
 * This controller is intentionally thin — it only maps the HTTP request to a service call.
 *
 * <p>Requirements: 6.1, 6.2, 6.5, 6.6
 */
@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final CsvImportService csvImportService;
    private final SmartImportService smartImportService;

    public ImportController(CsvImportService csvImportService,
                            SmartImportService smartImportService) {
        this.csvImportService = csvImportService;
        this.smartImportService = smartImportService;
    }

    /**
     * POST /api/v1/import/csv
     * Imports vocabulary words from an uploaded CSV file.
     *
     * <p>Requirement 6.1: parses a valid CSV (UTF-8, columns: word, phonetic, type, meaning, example)
     * and imports all valid rows. Returns {@code importedCount} and skipped-row details.
     *
     * <p>Requirement 6.2: when {@code groupId} is supplied, every successfully imported word
     * is associated with that group. Returns 404 if the group does not exist.
     *
     * <p>Requirement 6.5: response body contains {@code importedCount}, {@code groupId} (nullable),
     * and {@code skippedRows} (row number, word value, reason category).
     *
     * <p>Requirement 6.6: returns 400 if the file is not CSV, exceeds 1 MB, or contains
     * more than 500 data rows — enforced by {@link CsvImportService}.
     *
     * @param file    the multipart CSV file (field name {@code "file"})
     * @param groupId optional UUID of the vocabulary group to associate imported words with
     * @return 200 OK with the {@link ImportResponse} summary
     */
    @PostMapping(value = "/csv", consumes = "multipart/form-data")
    public ResponseEntity<ImportResponse> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID groupId) {

        ImportResponse response = csvImportService.importCsv(file, groupId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/import/analyze
     *
     * <p>Analyses an English text passage and returns a list of vocabulary suggestions
     * enriched with dictionary data (phonetic, part-of-speech, definition, example).
     * Words already in the user's vocabulary are flagged with {@code alreadyExists=true}.
     *
     * @param request the request body containing the text to analyse
     * @return 200 OK with a list of {@link SmartImportWordSuggestion}
     */
    @PostMapping("/analyze")
    public ResponseEntity<List<SmartImportWordSuggestion>> analyzeText(
            @Valid @RequestBody SmartImportRequest request) {

        List<SmartImportWordSuggestion> suggestions =
            smartImportService.analyzeText(request.getText());
        return ResponseEntity.ok(suggestions);
    }
}
