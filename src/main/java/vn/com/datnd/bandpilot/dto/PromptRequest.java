package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptRequest(
        @NotBlank String structureId,
        @NotBlank String structureTitle
) {
}
