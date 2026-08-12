package vn.com.datnd.bandpilot.dto;

import java.util.UUID;

/**
 * Summary item for the essay history list.
 * question is truncated to 120 characters with "..." if longer.
 */
public record EssayHistoryItemResponse(
        UUID id,
        String question,
        double overallBand,
        double taskAchievement,
        double coherenceCohesion,
        double lexicalResource,
        double grammaticalRange,
        String submittedAt
) {}
