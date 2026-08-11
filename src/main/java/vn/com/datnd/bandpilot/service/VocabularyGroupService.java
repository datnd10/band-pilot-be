package vn.com.datnd.bandpilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.com.datnd.bandpilot.dto.GroupDetailResponse;
import vn.com.datnd.bandpilot.dto.GroupRequest;
import vn.com.datnd.bandpilot.dto.GroupResponse;
import vn.com.datnd.bandpilot.dto.WordRequest;
import vn.com.datnd.bandpilot.dto.WordResponse;
import vn.com.datnd.bandpilot.entity.GroupWordMembership;
import vn.com.datnd.bandpilot.entity.GroupWordMembershipId;
import vn.com.datnd.bandpilot.entity.VocabularyGroup;
import vn.com.datnd.bandpilot.entity.WordEntry;
import vn.com.datnd.bandpilot.entity.WordExample;
import vn.com.datnd.bandpilot.exception.DuplicateResourceException;
import vn.com.datnd.bandpilot.exception.ExampleLimitExceededException;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.exception.ValidationException;import vn.com.datnd.bandpilot.repository.GroupWordMembershipRepository;
import vn.com.datnd.bandpilot.repository.VocabularyGroupRepository;
import vn.com.datnd.bandpilot.repository.WordEntryRepository;
import vn.com.datnd.bandpilot.repository.WordExampleRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for managing vocabulary groups and their word memberships.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Group name validation (1–100 chars, not blank)</li>
 *   <li>Case-insensitive name uniqueness enforcement</li>
 *   <li>Persisting {@link VocabularyGroup} and {@link GroupWordMembership} rows</li>
 *   <li>Deletion of groups including all memberships (without deleting {@link WordEntry} rows)</li>
 * </ul>
 * </p>
 */
@Service
public class VocabularyGroupService {

    private static final Logger log = LoggerFactory.getLogger(VocabularyGroupService.class);

    private final VocabularyGroupRepository groupRepository;
    private final GroupWordMembershipRepository membershipRepository;
    private final WordEntryRepository wordEntryRepository;
    private final WordExampleRepository wordExampleRepository;
    private final SrsService srsService;

    public VocabularyGroupService(VocabularyGroupRepository groupRepository,
                                   GroupWordMembershipRepository membershipRepository,
                                   WordEntryRepository wordEntryRepository,
                                   WordExampleRepository wordExampleRepository,
                                   SrsService srsService) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.wordEntryRepository = wordEntryRepository;
        this.wordExampleRepository = wordExampleRepository;
        this.srsService = srsService;
    }

    // ── Create ────────────────────────────────────────────────────────────────────

    /**
     * Creates a new vocabulary group.
     *
     * @param request the group data
     * @return the saved group as a response DTO (wordCount = 0)
     * @throws ValidationException        if name is blank or exceeds 100 characters
     * @throws DuplicateResourceException if a group with the same name (case-insensitive) already exists
     */
    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        validateGroupName(request.getName());

        if (groupRepository.findByNameIgnoreCase(request.getName().trim()).isPresent()) {
            throw new DuplicateResourceException(
                    "A vocabulary group already exists with the name: " + request.getName().trim());
        }

        VocabularyGroup group = new VocabularyGroup(request.getName().trim());
        VocabularyGroup saved = groupRepository.save(group);
        log.info("Group created: id={} name='{}'", saved.getId(), saved.getName());
        return toResponse(saved, 0);
    }

    // ── Read all ──────────────────────────────────────────────────────────────────

    /**
     * Returns all vocabulary groups with their word counts.
     *
     * @return list of all groups as response DTOs
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(group -> {
                    int count = membershipRepository.findByVocabularyGroup(group).size();
                    return toResponse(group, count);
                })
                .collect(Collectors.toList());
    }

    // ── Read by ID ────────────────────────────────────────────────────────────────

    /**
     * Retrieves a single vocabulary group by its UUID, including all its word entries.
     *
     * @param id the group's UUID
     * @return the group as a {@link GroupDetailResponse} with the full word list
     * @throws ResourceNotFoundException if no group exists with that ID
     */
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupById(UUID id) {
        VocabularyGroup group = findGroupOrThrow(id);
        List<GroupWordMembership> memberships = membershipRepository.findByVocabularyGroup(group);
        List<WordResponse> words = memberships.stream()
                .map(m -> toWordResponse(m.getWordEntry()))
                .collect(Collectors.toList());
        return toDetailResponse(group, words);
    }

    // ── Rename ────────────────────────────────────────────────────────────────────

    /**
     * Renames an existing vocabulary group.
     *
     * @param id      the group's UUID
     * @param request the new name
     * @return the updated group as a response DTO
     * @throws ResourceNotFoundException  if no group exists with that ID
     * @throws ValidationException        if the new name is blank or exceeds 100 characters
     * @throws DuplicateResourceException if another group already has the same name (case-insensitive)
     */
    @Transactional
    public GroupResponse renameGroup(UUID id, GroupRequest request) {
        validateGroupName(request.getName());

        VocabularyGroup group = findGroupOrThrow(id);

        if (groupRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new DuplicateResourceException(
                    "Another vocabulary group already exists with the name: " + request.getName().trim());
        }

        group.setName(request.getName().trim());
        VocabularyGroup saved = groupRepository.save(group);
        int count = membershipRepository.findByVocabularyGroup(saved).size();
        return toResponse(saved, count);
    }

    // ── Delete ────────────────────────────────────────────────────────────────────

    /**
     * Deletes a vocabulary group and all its word memberships.
     * Does NOT delete any {@link WordEntry} rows.
     *
     * @param id the group's UUID
     * @throws ResourceNotFoundException if no group exists with that ID
     */
    @Transactional
    public void deleteGroup(UUID id) {
        VocabularyGroup group = findGroupOrThrow(id);
        membershipRepository.deleteByVocabularyGroup(group);
        groupRepository.delete(group);
        log.info("Group deleted: id={} name='{}'", id, group.getName());
    }

    // ── Add word to group ─────────────────────────────────────────────────────────

    /**
     * Adds an existing word to a vocabulary group.
     *
     * @param groupId the group's UUID
     * @param wordId  the word's UUID
     * @return the updated group as a response DTO
     * @throws ResourceNotFoundException  if the word or group does not exist
     * @throws DuplicateResourceException if the word is already a member of the group
     */
    @Transactional
    public GroupResponse addWordToGroup(UUID groupId, UUID wordId) {
        WordEntry word = wordEntryRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("WordEntry", wordId));

        VocabularyGroup group = findGroupOrThrow(groupId);

        if (membershipRepository.existsByVocabularyGroupAndWordEntry(group, word)) {
            throw new DuplicateResourceException(
                    "Word is already a member of this group");
        }

        GroupWordMembership membership = new GroupWordMembership(group, word);
        membershipRepository.save(membership);
        srsService.initializeIfAbsent(word);

        int count = membershipRepository.findByVocabularyGroup(group).size();
        return toResponse(group, count);
    }

    // ── Remove word from group ────────────────────────────────────────────────────

    /**
     * Removes a word from a vocabulary group (disassociation only — WordEntry is preserved).
     *
     * @param groupId the group's UUID
     * @param wordId  the word's UUID
     * @throws ResourceNotFoundException if the group does not exist or the word is not in the group
     */
    @Transactional
    public void removeWordFromGroup(UUID groupId, UUID wordId) {
        VocabularyGroup group = findGroupOrThrow(groupId);

        WordEntry word = wordEntryRepository.findById(wordId)
                .orElseThrow(() -> new ResourceNotFoundException("WordEntry", wordId));

        if (!membershipRepository.existsByVocabularyGroupAndWordEntry(group, word)) {
            throw new ResourceNotFoundException(
                    "Word with id " + wordId + " is not a member of group with id " + groupId);
        }

        membershipRepository.deleteByVocabularyGroupAndWordEntry(group, word);
    }

    // ── Create word inside group ──────────────────────────────────────────────────

    /**
     * Creates a new WordEntry and immediately associates it with the given group.
     * This is the primary word-creation flow: words are always created inside a study set.
     *
     * @param groupId the group's UUID
     * @param request the new word data
     * @return the created word as a WordResponse
     * @throws ResourceNotFoundException  if the group does not exist
     * @throws ValidationException        if required fields are missing or invalid
     * @throws DuplicateResourceException if a word with the same spelling already exists
     */
    @Transactional
    public WordResponse createWordInGroup(UUID groupId, WordRequest request) {
        VocabularyGroup group = findGroupOrThrow(groupId);

        // Validate required fields
        if (request.getWord() == null || request.getWord().isBlank()) {
            throw new ValidationException("English word is required");
        }
        if (request.getWord().trim().length() > 100) {
            throw new ValidationException("Word must not exceed 100 characters");
        }
        if (request.getMeaning() == null || request.getMeaning().isBlank()) {
            throw new ValidationException("Meaning is required");
        }
        if (request.getMeaning().trim().length() > 500) {
            throw new ValidationException("Meaning must not exceed 500 characters");
        }

        // Duplicate detection
        if (wordEntryRepository.existsByWordIgnoreCase(request.getWord())) {
            throw new DuplicateResourceException(
                    "A word already exists with the spelling: " + request.getWord().trim());
        }

        // Save word
        WordEntry entry = new WordEntry(request.getWord().trim(), request.getMeaning().trim());
        entry.setPhonetic(request.getPhonetic() != null ? request.getPhonetic().trim() : null);
        entry.setType(request.getType() != null ? request.getType().trim().toLowerCase() : null);
        WordEntry saved = wordEntryRepository.save(entry);

        // Save examples (max 3)
        if (request.getExamples() != null && !request.getExamples().isEmpty()) {
            if (request.getExamples().size() > 3) {
                throw new ExampleLimitExceededException();
            }
            List<String> examples = request.getExamples();
            for (int i = 0; i < examples.size(); i++) {
                String sentence = examples.get(i);
                if (sentence != null && !sentence.isBlank()) {
                    WordExample ex = new WordExample(saved, sentence.trim(), (short) (i + 1));
                    wordExampleRepository.save(ex);
                }
            }
        }

        // Associate with group
        GroupWordMembership membership = new GroupWordMembership(group, saved);
        membershipRepository.save(membership);
        srsService.initializeIfAbsent(saved);

        return toWordResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /**
     * Validates that a group name is not blank and within the 100-character limit.
     */
    private void validateGroupName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Group name is required");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("Group name must not exceed 100 characters");
        }
    }

    /**
     * Finds a {@link VocabularyGroup} by ID or throws {@link ResourceNotFoundException}.
     */
    private VocabularyGroup findGroupOrThrow(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VocabularyGroup", id));
    }

    /**
     * Maps a {@link VocabularyGroup} to a {@link GroupResponse}.
     */
    private GroupResponse toResponse(VocabularyGroup group, int wordCount) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setWordCount(wordCount);
        response.setCreatedAt(group.getCreatedAt());
        return response;
    }

    /**
     * Maps a {@link VocabularyGroup} and its words to a {@link GroupDetailResponse}.
     */
    private GroupDetailResponse toDetailResponse(VocabularyGroup group, List<WordResponse> words) {
        GroupDetailResponse response = new GroupDetailResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setWordCount(words.size());
        response.setCreatedAt(group.getCreatedAt());
        response.setWords(words);
        return response;
    }

    /**
     * Maps a {@link WordEntry} to a {@link WordResponse}, including its example sentences.
     */
    private WordResponse toWordResponse(WordEntry entry) {
        WordResponse response = new WordResponse();
        response.setId(entry.getId());
        response.setWord(entry.getWord());
        response.setPhonetic(entry.getPhonetic());
        response.setType(entry.getType());
        response.setMeaning(entry.getMeaning());
        response.setStatus(entry.getStatus());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        List<String> sentences = wordExampleRepository
                .findByWordEntryOrderBySortOrderAsc(entry)
                .stream()
                .map(WordExample::getSentence)
                .collect(Collectors.toList());
        response.setExamples(sentences);
        return response;
    }
}
