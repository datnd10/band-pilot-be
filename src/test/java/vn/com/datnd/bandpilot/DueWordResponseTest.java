package vn.com.datnd.bandpilot;

import org.junit.jupiter.api.Test;
import vn.com.datnd.bandpilot.dto.DueWordResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DueWordResponse}.
 *
 * Tests verify:
 * - The new {@code examples} field exists and defaults to an empty list (Requirement 3.3)
 * - The backward-compat {@code example} (singular) field is still present (Requirement 3.4)
 * - Setting {@code examples} correctly replaces the list
 *
 * These tests run without a Spring context or database.
 */
class DueWordResponseTest {

    @Test
    void examples_defaultsToEmptyList() {
        DueWordResponse dto = new DueWordResponse();
        assertNotNull(dto.getExamples(), "examples list should never be null");
        assertTrue(dto.getExamples().isEmpty(), "examples list should default to empty");
    }

    @Test
    void example_singular_defaultsToNull() {
        DueWordResponse dto = new DueWordResponse();
        assertNull(dto.getExample(), "backward-compat example field should default to null");
    }

    @Test
    void setExamples_storesAllSentences() {
        DueWordResponse dto = new DueWordResponse();
        dto.setExamples(Arrays.asList("First sentence.", "Second sentence."));
        assertEquals(2, dto.getExamples().size());
        assertEquals("First sentence.", dto.getExamples().get(0));
        assertEquals("Second sentence.", dto.getExamples().get(1));
    }

    @Test
    void setExample_doesNotAffectExamplesList() {
        DueWordResponse dto = new DueWordResponse();
        dto.setExample("single example");
        dto.setExamples(Arrays.asList("a", "b", "c"));
        assertEquals("single example", dto.getExample());
        assertEquals(3, dto.getExamples().size());
    }

    @Test
    void sixArgConstructor_leavesExamplesEmpty() {
        UUID id = UUID.randomUUID();
        DueWordResponse dto = new DueWordResponse(id, "test", "meaning", "/fəˈnɛtɪk/", "noun", "example");
        assertEquals("example", dto.getExample());
        assertNotNull(dto.getExamples());
        // constructor leaves examples empty — caller must call setExamples
        assertTrue(dto.getExamples().isEmpty(),
                "6-arg constructor should leave examples empty (populated separately via setExamples)");
    }

    @Test
    void setExamples_emptyList_isAllowed() {
        DueWordResponse dto = new DueWordResponse();
        dto.setExamples(Collections.emptyList());
        assertNotNull(dto.getExamples());
        assertTrue(dto.getExamples().isEmpty());
    }
}
