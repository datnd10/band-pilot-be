package vn.com.datnd.bandpilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StructuredFeedbackResponse(
        @JsonProperty("structure_used") boolean structureUsed,
        @JsonProperty("errors") List<String> errors,
        @JsonProperty("suggestions") List<String> suggestions,
        @JsonProperty("model_sentence") String modelSentence,
        @JsonProperty("score") int score,
        @JsonProperty("encouragement") String encouragement
) {
}
