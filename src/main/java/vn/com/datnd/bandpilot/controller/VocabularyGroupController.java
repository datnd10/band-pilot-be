package vn.com.datnd.bandpilot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.com.datnd.bandpilot.dto.AddWordToGroupRequest;
import vn.com.datnd.bandpilot.dto.GroupDetailResponse;
import vn.com.datnd.bandpilot.dto.GroupRequest;
import vn.com.datnd.bandpilot.dto.GroupResponse;
import vn.com.datnd.bandpilot.dto.ImportResponse;
import vn.com.datnd.bandpilot.dto.WordRequest;
import vn.com.datnd.bandpilot.dto.WordResponse;
import vn.com.datnd.bandpilot.service.CsvImportService;
import vn.com.datnd.bandpilot.service.VocabularyGroupService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for vocabulary group CRUD and membership operations.
 *
 * <p>All error handling (400, 404, 409) is delegated to
 * {@link vn.com.datnd.bandpilot.exception.GlobalExceptionHandler}.
 * This controller is intentionally thin — it only maps HTTP requests to service calls.
 *
 * <p>Requirements: 7.1, 7.3, 7.4, 7.6, 7.7, 7.8, 7.9
 */
@RestController
@RequestMapping("/api/v1/groups")
public class VocabularyGroupController {

    private final VocabularyGroupService groupService;
    private final CsvImportService csvImportService;

    public VocabularyGroupController(VocabularyGroupService groupService,
                                     CsvImportService csvImportService) {
        this.groupService = groupService;
        this.csvImportService = csvImportService;
    }

    /**
     * GET /api/v1/groups
     * Returns all vocabulary groups with their word counts.
     *
     * <p>Requirement 7.3
     *
     * @return 200 OK with list of all groups
     */
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups() {
        List<GroupResponse> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * POST /api/v1/groups
     * Creates a new vocabulary group.
     *
     * <p>Requirement 7.1: name must be 1–100 characters and unique (case-insensitive).
     * Returns 409 if a group with the same name already exists.
     * Returns 400 if name is blank or exceeds 100 characters.
     *
     * @param request the group name
     * @return 201 Created with the new group (wordCount = 0)
     */
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request) {
        GroupResponse created = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/v1/groups/{id}
     * Retrieves a single vocabulary group by its UUID, including its word list.
     *
     * <p>Requirement 7.4: returns the group with its word list.
     * Returns 404 if the group does not exist.
     *
     * @param id the group's UUID
     * @return 200 OK with the group and words, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupDetailResponse> getGroupById(@PathVariable UUID id) {
        GroupDetailResponse group = groupService.getGroupById(id);
        return ResponseEntity.ok(group);
    }

    /**
     * PUT /api/v1/groups/{id}
     * Renames an existing vocabulary group.
     *
     * <p>Requirement 7.8: new name must be 1–100 characters and unique (case-insensitive).
     * Returns 404 if the group does not exist.
     * Returns 409 if another group already has the same name.
     * Returns 400 if name is blank or exceeds 100 characters.
     *
     * @param id      the group's UUID
     * @param request the new name
     * @return 200 OK with the updated group
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> renameGroup(
            @PathVariable UUID id,
            @RequestBody GroupRequest request) {

        GroupResponse updated = groupService.renameGroup(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/groups/{id}
     * Deletes a vocabulary group and all its word memberships.
     * Does NOT delete the associated WordEntry rows.
     *
     * <p>Requirement 7.9
     * Returns 404 if the group does not exist.
     *
     * @param id the group's UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/groups/{id}/words
     * Adds an existing word (by ID) to a vocabulary group.
     *
     * <p>Requirement 7.6: validates word ID exists, group ID exists, not already a member.
     * Returns 404 if the word or group does not exist.
     * Returns 409 if the word is already a member of the group.
     *
     * @param id      the group's UUID
     * @param request body containing the wordId to add
     * @return 200 OK with the updated group (incremented wordCount)
     */
    @PostMapping("/{id}/words")
    public ResponseEntity<GroupResponse> addWordToGroup(
            @PathVariable UUID id,
            @RequestBody AddWordToGroupRequest request) {

        GroupResponse updated = groupService.addWordToGroup(id, request.getWordId());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/groups/{id}/words/{wordId}
     * Removes a word from a vocabulary group (disassociation only — WordEntry is preserved).
     */
    @DeleteMapping("/{id}/words/{wordId}")
    public ResponseEntity<Void> removeWordFromGroup(
            @PathVariable UUID id,
            @PathVariable UUID wordId) {

        groupService.removeWordFromGroup(id, wordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/groups/{id}/words/new
     * Creates a brand-new word entry and immediately adds it to this group.
     * This is the primary word-creation flow: words are always created inside a study set.
     *
     * @param id      the group's UUID
     * @param request the new word data (word + meaning required)
     * @return 201 Created with the new WordResponse
     */
    @PostMapping("/{id}/words/new")
    public ResponseEntity<WordResponse> createWordInGroup(
            @PathVariable UUID id,
            @RequestBody WordRequest request) {

        WordResponse created = groupService.createWordInGroup(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/v1/groups/{id}/import
     * Imports vocabulary words from a CSV file and associates them with this group.
     * The group must exist; returns 404 otherwise.
     *
     * @param id   the group's UUID
     * @param file the multipart CSV file (field name "file")
     * @return 200 OK with ImportResponse summary
     */
    @PostMapping(value = "/{id}/import", consumes = "multipart/form-data")
    public ResponseEntity<ImportResponse> importCsvToGroup(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        ImportResponse response = csvImportService.importCsv(file, id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/groups/{id}/import/text
     * Imports vocabulary words from pipe-delimited plain text.
     * Each line: word|meaning|phonetic|type|example
     * Only word and meaning are required.
     */
    @PostMapping(value = "/{id}/import/text", consumes = "text/plain;charset=UTF-8")
    public ResponseEntity<ImportResponse> importTextToGroup(
            @PathVariable UUID id,
            @RequestBody String text) {

        ImportResponse response = csvImportService.importText(text, id);
        return ResponseEntity.ok(response);
    }
}
