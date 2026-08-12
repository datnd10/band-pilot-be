package vn.com.datnd.bandpilot.dto;

import java.util.List;

public record EssayScoreResponse(
        double taskAchievement,
        double coherenceCohesion,
        double lexicalResource,
        double grammaticalRange,
        double overallBand,
        List<String> strengths,
        List<String> improvements,
        String improvedVersion,
        String encouragement
) {}
