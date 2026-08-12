package vn.com.datnd.bandpilot.dto;

import java.util.List;
import java.util.UUID;

/**
 * Full detail response for a single essay submission, including feedback content.
 */
public record EssayHistoryDetailResponse(
        UUID id,
        String question,
        double overallBand,
        double taskAchievement,
        double coherenceCohesion,
        double lexicalResource,
        double grammaticalRange,
        String submittedAt,
        String essay,
        List<String> strengths,
        List<String> improvements,
        String improvedVersion,
        String encouragement
) {}
