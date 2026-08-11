package vn.com.datnd.bandpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the Smart Import text-analysis endpoint.
 */
public class SmartImportRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 5000, message = "Text must not exceed 5000 characters")
    private String text;

    public SmartImportRequest() {
    }

    public SmartImportRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
