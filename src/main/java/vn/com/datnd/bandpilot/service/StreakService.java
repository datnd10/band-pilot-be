package vn.com.datnd.bandpilot.service;

import org.springframework.stereotype.Service;
import vn.com.datnd.bandpilot.dto.StreakResponse;
import vn.com.datnd.bandpilot.repository.ReviewSessionRepository;
import vn.com.datnd.bandpilot.repository.TypingSessionRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Computes streak metrics on-the-fly from ReviewSession and TypingSession completedAt timestamps.
 *
 * <p>All day-boundary calculations use the {@code Asia/Ho_Chi_Minh} timezone (UTC+7).</p>
 */
@Service
public class StreakService {

    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final List<Integer> BADGE_THRESHOLDS = List.of(3, 7, 14, 30, 60, 100);

    private final ReviewSessionRepository reviewRepo;
    private final TypingSessionRepository  typingRepo;

    public StreakService(ReviewSessionRepository reviewRepo, TypingSessionRepository typingRepo) {
        this.reviewRepo = reviewRepo;
        this.typingRepo = typingRepo;
    }

    // ── Public API ────────────────────────────────────────────────────────────────

    /**
     * Returns the full streak summary for the given user.
     *
     * @param userId the user whose streak to compute
     * @return streak response with currentStreak, longestStreak, activeDaysLast30, badges
     */
    public StreakResponse getStreak(UUID userId) {
        Set<LocalDate> activeDays = collectActiveDays(userId);
        LocalDate today = LocalDate.now(VN_ZONE);

        int currentStreak    = computeCurrentStreak(activeDays, today);
        int longestStreak    = computeLongestStreak(activeDays);
        int activeDaysLast30 = computeActiveDaysLast30(activeDays, today);
        List<String> badges  = deriveBadges(longestStreak);

        return new StreakResponse(currentStreak, longestStreak, activeDaysLast30, badges);
    }

    // ── Package-visible helpers (for unit testing) ────────────────────────────────

    /**
     * Counts consecutive active days ending at {@code today} (or yesterday if today is not active).
     */
    public int computeCurrentStreak(Set<LocalDate> activeDays, LocalDate today) {
        // Start from today; if today has no session, start from yesterday
        LocalDate cursor = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * Returns the length of the longest consecutive run across all active days.
     */
    public int computeLongestStreak(Set<LocalDate> activeDays) {
        if (activeDays.isEmpty()) return 0;
        List<LocalDate> sorted = activeDays.stream().sorted().toList();
        int longest = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).minusDays(1).equals(sorted.get(i - 1))) {
                current++;
                if (current > longest) longest = current;
            } else {
                current = 1;
            }
        }
        return longest;
    }

    /**
     * Counts distinct active days within the 30-day window ending {@code today} (inclusive).
     */
    public int computeActiveDaysLast30(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate start = today.minusDays(29); // 30 days inclusive: today - 29 .. today
        return (int) activeDays.stream()
                               .filter(d -> !d.isBefore(start) && !d.isAfter(today))
                               .count();
    }

    /**
     * Returns badge identifiers for all thresholds ≤ {@code longestStreak}.
     */
    public List<String> deriveBadges(int longestStreak) {
        return BADGE_THRESHOLDS.stream()
                               .filter(t -> longestStreak >= t)
                               .map(t -> "STREAK_" + t)
                               .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private Set<LocalDate> collectActiveDays(UUID userId) {
        Set<LocalDate> days = new HashSet<>();
        addDates(days, reviewRepo.findAllCompletedAtByUserId(userId));
        addDates(days, typingRepo.findAllCompletedAtByUserId(userId));
        return days;
    }

    private void addDates(Set<LocalDate> days, List<Instant> instants) {
        for (Instant instant : instants) {
            days.add(instant.atZone(VN_ZONE).toLocalDate());
        }
    }
}
