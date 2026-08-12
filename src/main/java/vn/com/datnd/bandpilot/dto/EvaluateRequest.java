package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluateRequest(
        @NotBlank String structureId,
        @NotBlank String structureTitle,
        @NotBlank String prompt,
        @NotBlank String userResponse
) {
}
