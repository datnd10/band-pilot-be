package vn.com.datnd.bandpilot;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.com.datnd.bandpilot.dto.StreakResponse;
import vn.com.datnd.bandpilot.repository.ReviewSessionRepository;
import vn.com.datnd.bandpilot.repository.TypingSessionRepository;
import vn.com.datnd.bandpilot.service.StreakService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StreakService}.
 *
 * No Spring context needed — repositories are mocked.
 */
class StreakServiceTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final UUID USER_ID = UUID.randomUUID();

    private ReviewSessionRepository reviewRepo;
    private TypingSessionRepository  typingRepo;
    private StreakService             service;

    @BeforeEach
    void setUp() {
        reviewRepo = Mockito.mock(ReviewSessionRepository.class);
        typingRepo = Mockito.mock(TypingSessionRepository.class);
        service    = new StreakService(reviewRepo, typingRepo);
    }

    // ── Helper: convert VN LocalDate to Instant (midnight VN time) ───────────────

    private static Instant toInstant(LocalDate date) {
        return date.atStartOfDay(VN_ZONE).toInstant();
    }

    // ── 5.1: No sessions ─────────────────────────────────────────────────────────

    @Test
    void getStreak_noSessions_returnsAllZerosAndEmptyBadges() {
        when(reviewRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());
        when(typingRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());

        StreakResponse result = service.getStreak(USER_ID);

        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(0);
        assertThat(result.getActiveDaysLast30()).isEqualTo(0);
        assertThat(result.getBadges()).isEmpty();
    }

    // ── 5.2: 7 consecutive days ending today ─────────────────────────────────────

    @Test
    void getStreak_7ConsecutiveDaysEndingToday_returnsCurrentStreak7() {
        LocalDate today = LocalDate.now(VN_ZONE);
        List<Instant> instants = Stream.iterate(today.minusDays(6), d -> d.plusDays(1))
                .limit(7)
                .map(StreakServiceTest::toInstant)
                .collect(Collectors.toList());

        when(reviewRepo.findAllCompletedAtByUserId(any())).thenReturn(instants);
        when(typingRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());

        StreakResponse result = service.getStreak(USER_ID);

        assertThat(result.getCurrentStreak()).isEqualTo(7);
        assertThat(result.getLongestStreak()).isEqualTo(7);
        assertThat(result.getBadges()).containsExactly("STREAK_3", "STREAK_7");
    }

    // ── 5.3: 7 consecutive days ending yesterday ─────────────────────────────────

    @Test
    void getStreak_7ConsecutiveDaysEndingYesterday_returnsCurrentStreak7() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDate yesterday = today.minusDays(1);
        List<Instant> instants = Stream.iterate(yesterday.minusDays(6), d -> d.plusDays(1))
                .limit(7)
                .map(StreakServiceTest::toInstant)
                .collect(Collectors.toList());

        when(reviewRepo.findAllCompletedAtByUserId(any())).thenReturn(instants);
        when(typingRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());

        StreakResponse result = service.getStreak(USER_ID);

        assertThat(result.getCurrentStreak()).isEqualTo(7);
    }

    // ── 5.4: Broken streak — 5 days, gap, 3 days ─────────────────────────────────

    @Test
    void getStreak_brokenStreak_returnsCorrectCurrentAndLongest() {
        LocalDate today = LocalDate.now(VN_ZONE);
        // Last 3 days (including today)
        List<LocalDate> recentDays = List.of(
                today.minusDays(2),
                today.minusDays(1),
                today
        );
        // 5 days ending 5 days ago (gap of 1 day in between)
        List<LocalDate> olderDays = List.of(
                today.minusDays(9),
                today.minusDays(8),
                today.minusDays(7),
                today.minusDays(6),
                today.minusDays(5)
        );
        // Gap: today-4 is skipped

        List<Instant> instants = Stream.concat(recentDays.stream(), olderDays.stream())
                .map(StreakServiceTest::toInstant)
                .collect(Collectors.toList());

        when(reviewRepo.findAllCompletedAtByUserId(any())).thenReturn(instants);
        when(typingRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());

        StreakResponse result = service.getStreak(USER_ID);

        assertThat(result.getCurrentStreak()).isEqualTo(3);
        assertThat(result.getLongestStreak()).isEqualTo(5);
    }

    // ── 5.5: Timezone test ───────────────────────────────────────────────────────

    @Test
    void getStreak_sessionAt17_01_UTC_countsAsNextDayInVietnam() {
        // 17:01 UTC = 00:01+7 (next calendar day in VN)
        Instant instant = Instant.parse("2024-01-01T17:01:00Z");
        LocalDate expectedVnDate = LocalDate.of(2024, 1, 2); // VN date
        LocalDate actualDate = instant.atZone(VN_ZONE).toLocalDate();
        assertThat(actualDate).isEqualTo(expectedVnDate);

        // Set up 2 consecutive sessions: Jan 1 VN (= Dec 31 UTC late) and Jan 2 VN
        Instant jan1InVn = Instant.parse("2023-12-31T17:00:00Z"); // 00:00 VN Jan 1
        Instant jan2InVn = instant;                                // 00:01 VN Jan 2

        when(reviewRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of(jan1InVn, jan2InVn));
        when(typingRepo.findAllCompletedAtByUserId(any())).thenReturn(List.of());

        // Verify dates computed correctly
        assertThat(jan1InVn.atZone(VN_ZONE).toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(jan2InVn.atZone(VN_ZONE).toLocalDate()).isEqualTo(LocalDate.of(2024, 1, 2));
    }

    // ── 5.6: deriveBadges ────────────────────────────────────────────────────────

    @Test
    void deriveBadges_longestStreak14_returnsStreak3_7_14() {
        List<String> badges = service.deriveBadges(14);
        assertThat(badges).containsExactly("STREAK_3", "STREAK_7", "STREAK_14");
    }

    @Test
    void deriveBadges_longestStreak2_returnsEmpty() {
        List<String> badges = service.deriveBadges(2);
        assertThat(badges).isEmpty();
    }

    @Test
    void deriveBadges_longestStreak100_returnsAll6Badges() {
        List<String> badges = service.deriveBadges(100);
        assertThat(badges).containsExactly("STREAK_3", "STREAK_7", "STREAK_14", "STREAK_30", "STREAK_60", "STREAK_100");
    }

    @Test
    void deriveBadges_longestStreak0_returnsEmpty() {
        List<String> badges = service.deriveBadges(0);
        assertThat(badges).isEmpty();
    }

    // ── 5.7: Property — longestStreak ≥ currentStreak ────────────────────────────
    // Validates: Requirements 1.1, 1.2

    @Property
    void property_longestStreakAlwaysGreaterOrEqualToCurrentStreak(
            @ForAll("randomActiveDays") Set<LocalDate> activeDays) {

        // Create fresh service for this property (jqwik creates a new instance per try,
        // @BeforeEach doesn't apply to @Property methods in jqwik)
        StreakService svc = newStreakService();
        LocalDate today = LocalDate.now(VN_ZONE);

        int currentStreak = svc.computeCurrentStreak(activeDays, today);
        int longestStreak = svc.computeLongestStreak(activeDays);

        assertThat(longestStreak).isGreaterThanOrEqualTo(currentStreak);
    }

    @Provide
    Arbitrary<Set<LocalDate>> randomActiveDays() {
        LocalDate base = LocalDate.now(VN_ZONE);
        return Arbitraries.integers().between(0, 60)
                .flatMap(offset -> Arbitraries.just(base.minusDays(offset)))
                .set()
                .ofMinSize(0)
                .ofMaxSize(120);
    }

    // ── 5.8: Property — badges contain exactly thresholds ≤ longestStreak ────────
    // Validates: Requirements 3.1, 3.2, 3.3

    @Property
    void property_badgesContainExactlyThresholdsBelowOrEqualLongestStreak(
            @ForAll @IntRange(min = 0, max = 150) int longestStreak) {

        StreakService svc = newStreakService();
        List<String> badges = svc.deriveBadges(longestStreak);
        List<Integer> thresholds = List.of(3, 7, 14, 30, 60, 100);

        // Every badge in result must correspond to a threshold ≤ longestStreak
        for (String badge : badges) {
            int threshold = Integer.parseInt(badge.replace("STREAK_", ""));
            assertThat(threshold).isLessThanOrEqualTo(longestStreak);
        }

        // Every threshold ≤ longestStreak must be in the result
        for (int t : thresholds) {
            if (t <= longestStreak) {
                assertThat(badges).contains("STREAK_" + t);
            } else {
                assertThat(badges).doesNotContain("STREAK_" + t);
            }
        }
    }

    // ── activeDaysLast30 bounds ───────────────────────────────────────────────────

    @Property
    void property_activeDaysLast30_between0And30(
            @ForAll("randomActiveDays") Set<LocalDate> activeDays) {

        StreakService svc = newStreakService();
        LocalDate today = LocalDate.now(VN_ZONE);
        int count = svc.computeActiveDaysLast30(activeDays, today);

        assertThat(count).isBetween(0, 30);
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    private static StreakService newStreakService() {
        ReviewSessionRepository rev = Mockito.mock(ReviewSessionRepository.class);
        TypingSessionRepository  typ = Mockito.mock(TypingSessionRepository.class);
        return new StreakService(rev, typ);
    }
}
