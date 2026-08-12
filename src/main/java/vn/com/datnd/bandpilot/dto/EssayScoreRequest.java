package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotBlank;

public record EssayScoreRequest(
        @NotBlank String question,
        @NotBlank String essay
) {}
