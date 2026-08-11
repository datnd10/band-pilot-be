package vn.com.datnd.bandpilot.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.com.datnd.bandpilot.dto.ImportResponse;
import vn.com.datnd.bandpilot.dto.ImportResponse.SkippedRow;
import vn.com.datnd.bandpilot.entity.GroupWordMembership;
import vn.com.datnd.bandpilot.entity.VocabularyGroup;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.entity.WordExample;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.exception.ValidationException;
import vn.com.datnd.bandpilot.repository.GroupWordMembershipRepository;
import vn.com.datnd.bandpilot.repository.VocabularyGroupRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;
import vn.com.datnd.bandpilot.repository.WordExampleRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles bulk import of vocabulary words from a CSV file.
 *
 * <p>CSV format (header row required):
 * {@code word, phonetic, type, meaning, example}
 *
 * <p>Pre-processing validations (abort before processing rows):
 * <ul>
 *   <li>File must be CSV (Content-Type {@code text/csv} OR filename ends with {@code .csv})</li>
 *   <li>File size must be ≤ 1 MB (1,048,576 bytes)</li>
 *   <li>Data row count must be 1–500</li>
 * </ul>
 *
 * <p>Per-row validations (skip row, never abort):
 * <ul>
 *   <li>{@code word} or {@code meaning} blank/missing → skip with {@code MISSING_REQUIRED_FIELD}</li>
 *   <li>Word already exists in DB or already imported in this batch → skip with {@code DUPLICATE_WORD}</li>
 * </ul>
 */
@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final long MAX_FILE_SIZE = 1_048_576L; // 1 MB
    private static final int MAX_ROWS = 500;

    private final WordEntryRepository wordEntryRepository;
    private final WordExampleRepository wordExampleRepository;
    private final VocabularyGroupRepository vocabularyGroupRepository;
    private final GroupWordMembershipRepository groupWordMembershipRepository;
    private final SrsService srsService;

    public CsvImportService(WordEntryRepository wordEntryRepository,
                             WordExampleRepository wordExampleRepository,
                             VocabularyGroupRepository vocabularyGroupRepository,
                             GroupWordMembershipRepository groupWordMembershipRepository,
                             SrsService srsService) {
        this.wordEntryRepository = wordEntryRepository;
        this.wordExampleRepository = wordExampleRepository;
        this.vocabularyGroupRepository = vocabularyGroupRepository;
        this.groupWordMembershipRepository = groupWordMembershipRepository;
        this.srsService = srsService;
    }

    /**
     * Imports vocabulary words from the given CSV file, optionally associating them
     * with a vocabulary group.
     *
     * @param file    the uploaded CSV file
     * @param groupId optional UUID of the group to associate imported words with
     * @return an {@link ImportResponse} with counts and details of any skipped rows
     * @throws ValidationException       if the file fails format, size, or row-count validation
     * @throws ResourceNotFoundException if {@code groupId} is provided but no group exists with that ID
     */
    @Transactional
    public ImportResponse importCsv(MultipartFile file, UUID groupId) {
        // ── 1. Pre-processing: file format validation ─────────────────────────────
        validateFileFormat(file);

        // ── 2. Pre-processing: file size validation ───────────────────────────────
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size must not exceed 1 MB");
        }

        // ── 3. Validate group exists before touching any rows ─────────────────────
        VocabularyGroup group = null;
        if (groupId != null) {
            group = vocabularyGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("VocabularyGroup", groupId));
        }

        // ── 4. Parse CSV and collect all data rows ────────────────────────────────
        List<String[]> dataRows = parseCsvRows(file);

        // ── 5. Pre-processing: row count validation ───────────────────────────────
        if (dataRows.isEmpty()) {
            throw new ValidationException("File contains no importable data");
        }
        if (dataRows.size() > MAX_ROWS) {
            throw new ValidationException("Row limit exceeded: file contains " + dataRows.size()
                    + " data rows but the maximum allowed is " + MAX_ROWS);
        }

        // ── 6. Process rows ───────────────────────────────────────────────────────
        List<SkippedRow> skippedRows = new ArrayList<>();
        // Tracks words imported in this batch (lower-case) to detect intra-batch duplicates
        Set<String> importedInBatch = new HashSet<>();
        int importedCount = 0;

        for (int i = 0; i < dataRows.size(); i++) {
            int rowNumber = i + 1; // 1-based, header not counted
            String[] columns = dataRows.get(i);

            String word = getColumn(columns, 0);
            String phonetic = getColumn(columns, 1);
            String type = getColumn(columns, 2);
            String meaning = getColumn(columns, 3);
            String example = getColumn(columns, 4);

            // Per-row validation: required fields
            if (word == null || word.isBlank() || meaning == null || meaning.isBlank()) {
                skippedRows.add(new SkippedRow(rowNumber, word != null ? word : "", "MISSING_REQUIRED_FIELD"));
                continue;
            }

            word = word.trim();
            meaning = meaning.trim();

            // Per-row validation: duplicate detection
            String wordLower = word.toLowerCase();
            if (importedInBatch.contains(wordLower) || wordEntryRepository.existsByWordIgnoreCase(word)) {
                skippedRows.add(new SkippedRow(rowNumber, word, "DUPLICATE_WORD"));
                continue;
            }

            // Save the word entry
            WordEntry entry = new WordEntry(word, meaning);
            if (phonetic != null && !phonetic.isBlank()) {
                entry.setPhonetic(phonetic.trim());
            }
            if (type != null && !type.isBlank()) {
                entry.setType(type.trim().toLowerCase());
            }
            WordEntry saved = wordEntryRepository.save(entry);
            srsService.initializeIfAbsent(saved);

            // Save example sentence if present
            if (example != null && !example.isBlank()) {
                WordExample ex = new WordExample(saved, example.trim(), (short) 1);
                wordExampleRepository.save(ex);
            }

            // Associate with group if provided
            if (group != null) {
                GroupWordMembership membership = new GroupWordMembership(group, saved);
                groupWordMembershipRepository.save(membership);
            }

            importedInBatch.add(wordLower);
            importedCount++;
        }

        return new ImportResponse(importedCount, groupId, skippedRows);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Validates that the file is a CSV based on Content-Type or filename extension.
     */
    private void validateFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File must not be empty");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean csvContentType = contentType != null && contentType.equalsIgnoreCase("text/csv");
        boolean csvExtension = originalFilename != null
                && originalFilename.toLowerCase().endsWith(".csv");

        if (!csvContentType && !csvExtension) {
            throw new ValidationException(
                    "Invalid file format: only CSV files are accepted (Content-Type: text/csv or .csv extension)");
        }
    }

    /**
     * Parses the CSV file and returns all data rows (the header row is consumed and discarded).
     * The header row must contain at minimum a {@code word} column.
     */
    private List<String[]> parseCsvRows(MultipartFile file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read and validate the header row
            String[] header = reader.readNext();
            if (header == null) {
                throw new ValidationException("File contains no importable data");
            }

            // Build a column index map from the header (case-insensitive)
            Map<String, Integer> columnIndex = buildColumnIndex(header);
            validateRequiredColumns(columnIndex);

            // Collect data rows, reordering columns to match expected order: word, phonetic, type, meaning, example
            List<String[]> dataRows = new ArrayList<>();
            String[] row;
            while ((row = reader.readNext()) != null) {
                // Skip completely blank lines
                if (isBlankRow(row)) {
                    continue;
                }
                String[] normalized = new String[5];
                normalized[0] = getColumnByName(row, columnIndex, "word");
                normalized[1] = getColumnByName(row, columnIndex, "phonetic");
                normalized[2] = getColumnByName(row, columnIndex, "type");
                normalized[3] = getColumnByName(row, columnIndex, "meaning");
                normalized[4] = getColumnByName(row, columnIndex, "example");
                dataRows.add(normalized);
            }
            return dataRows;

        } catch (IOException | CsvValidationException e) {
            throw new ValidationException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    /**
     * Builds a case-insensitive map of column name → column index from the header row.
     */
    private Map<String, Integer> buildColumnIndex(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] != null) {
                index.put(header[i].trim().toLowerCase(), i);
            }
        }
        return index;
    }

    /**
     * Validates that the header contains the required {@code word} and {@code meaning} columns.
     */
    private void validateRequiredColumns(Map<String, Integer> columnIndex) {
        if (!columnIndex.containsKey("word") || !columnIndex.containsKey("meaning")) {
            throw new ValidationException(
                    "CSV header must contain 'word' and 'meaning' columns");
        }
    }

    /**
     * Returns the trimmed value from a row at the given column name, or {@code null} if absent.
     */
    private String getColumnByName(String[] row, Map<String, Integer> columnIndex, String name) {
        Integer idx = columnIndex.get(name);
        if (idx == null || idx >= row.length) {
            return null;
        }
        String val = row[idx];
        return val != null ? val.trim() : null;
    }

    /**
     * Returns the trimmed value at the given positional index, or {@code null} if out of bounds.
     */
    private String getColumn(String[] row, int index) {
        if (index >= row.length) {
            return null;
        }
        String val = row[index];
        return val != null ? val.trim() : null;
    }

    /**
     * Returns {@code true} if every cell in the row is null or blank.
     */
    private boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.isBlank()) {
                return false;
            }
        }
        return true;
    }

    // ── Text import (pipe-delimited) ──────────────────────────────────────────────

    /**
     * Imports vocabulary words from plain pipe-delimited text.
     *
     * <p>Each non-blank line must follow the format:
     * {@code word|meaning|phonetic|type|example}
     * Only {@code word} and {@code meaning} are required; all other fields are optional.
     *
     * @param text    the raw pipe-delimited text (one entry per line)
     * @param groupId the group to associate imported words with (must exist)
     * @return an {@link ImportResponse} with counts and details of any skipped rows
     * @throws ValidationException       if the text is blank or exceeds 500 lines
     * @throws ResourceNotFoundException if the group does not exist
     */
    @Transactional
    public ImportResponse importText(String text, UUID groupId) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Text is required");
        }

        // Validate group
        VocabularyGroup group = vocabularyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("VocabularyGroup", groupId));

        // Split into non-blank lines
        String[] lines = text.split("\\r?\\n");
        List<String> dataLines = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                dataLines.add(line.trim());
            }
        }

        if (dataLines.isEmpty()) {
            throw new ValidationException("No importable data found in text");
        }
        if (dataLines.size() > MAX_ROWS) {
            throw new ValidationException("Row limit exceeded: " + dataLines.size()
                    + " lines found but maximum allowed is " + MAX_ROWS);
        }

        List<SkippedRow> skippedRows = new ArrayList<>();
        Set<String> importedInBatch = new HashSet<>();
        int importedCount = 0;

        for (int i = 0; i < dataLines.size(); i++) {
            int rowNumber = i + 1;
            String line = dataLines.get(i);
            String[] parts = line.split("\\|", -1);

            String word    = parts.length > 0 ? parts[0].trim() : null;
            String meaning = parts.length > 1 ? parts[1].trim() : null;
            String phonetic = parts.length > 2 ? parts[2].trim() : null;
            String type    = parts.length > 3 ? parts[3].trim() : null;
            String example = parts.length > 4 ? parts[4].trim() : null;

            if (word == null || word.isBlank() || meaning == null || meaning.isBlank()) {
                skippedRows.add(new SkippedRow(rowNumber, word != null ? word : "", "MISSING_REQUIRED_FIELD"));
                continue;
            }

            String wordLower = word.toLowerCase();
            if (importedInBatch.contains(wordLower) || wordEntryRepository.existsByWordIgnoreCase(word)) {
                skippedRows.add(new SkippedRow(rowNumber, word, "DUPLICATE_WORD"));
                continue;
            }

            WordEntry entry = new WordEntry(word, meaning);
            if (phonetic != null && !phonetic.isBlank()) entry.setPhonetic(phonetic);
            if (type != null && !type.isBlank()) entry.setType(type.toLowerCase());
            WordEntry saved = wordEntryRepository.save(entry);
            srsService.initializeIfAbsent(saved);

            if (example != null && !example.isBlank()) {
                WordExample ex = new WordExample(saved, example, (short) 1);
                wordExampleRepository.save(ex);
            }

            GroupWordMembership membership = new GroupWordMembership(group, saved);
            groupWordMembershipRepository.save(membership);

            importedInBatch.add(wordLower);
            importedCount++;
        }

        log.info("Text import complete: groupId={} imported={} skipped={}", groupId, importedCount, skippedRows.size());
        return new ImportResponse(importedCount, groupId, skippedRows);
    }
}
