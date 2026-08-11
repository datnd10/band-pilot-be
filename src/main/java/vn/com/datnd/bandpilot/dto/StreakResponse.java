package vn.com.datnd.bandpilot.dto;

import java.util.List;

/**
 * Response DTO for the streak & gamification endpoint.
 */
public class StreakResponse {

    private final int currentStreak;
    private final int longestStreak;
    private final int activeDaysLast30;
    private final List<String> badges;

    public StreakResponse(int currentStreak, int longestStreak, int activeDaysLast30, List<String> badges) {
        this.currentStreak    = currentStreak;
        this.longestStreak    = longestStreak;
        this.activeDaysLast30 = activeDaysLast30;
        this.badges           = badges;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public int getActiveDaysLast30() {
        return activeDaysLast30;
    }

    public List<String> getBadges() {
        return badges;
    }
}
