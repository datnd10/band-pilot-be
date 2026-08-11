package vn.com.datnd.bandpilot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vn.com.datnd.bandpilot.dto.Rating;
import vn.com.datnd.bandpilot.entity.ReviewSessionWordResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the unknownCount → Rating mapping logic in ReviewSessionWordResult.
 *
 * No Spring context needed — these tests exercise pure static logic only.
 */
class ReviewSessionRatingMappingTest {

    @Test
    void unknownCount_0_maps_to_EASY() {
        assertThat(ReviewSessionWordResult.deriveRating(0)).isEqualTo(Rating.EASY);
    }

    @Test
    void unknownCount_1_maps_to_GOOD() {
        assertThat(ReviewSessionWordResult.deriveRating(1)).isEqualTo(Rating.GOOD);
    }

    @Test
    void unknownCount_2_maps_to_AGAIN() {
        assertThat(ReviewSessionWordResult.deriveRating(2)).isEqualTo(Rating.AGAIN);
    }

    @Test
    void unknownCount_large_maps_to_AGAIN() {
        assertThat(ReviewSessionWordResult.deriveRating(100)).isEqualTo(Rating.AGAIN);
    }

    @ParameterizedTest
    @CsvSource({
        "0,  EASY",
        "1,  GOOD",
        "2,  AGAIN",
        "3,  AGAIN",
        "10, AGAIN",
    })
    void parameterized_rating_mapping(int unknownCount, Rating expected) {
        assertThat(ReviewSessionWordResult.deriveRating(unknownCount)).isEqualTo(expected);
    }
}
