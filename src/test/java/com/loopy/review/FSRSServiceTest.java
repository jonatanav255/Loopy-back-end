package com.loopy.review;

import com.loopy.review.service.FSRSService;
import com.loopy.review.service.SchedulingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FSRS-5 algorithm — no Spring context needed.
 */
class FSRSServiceTest {

    private FSRSService fsrsService;
    private final LocalDate today = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        fsrsService = new FSRSService();
    }

    @Test
    void newCard_goodRating_setsStabilityAndDifficulty() {
        SchedulingResult result = fsrsService.calculate(0, 0, 0, 4, 0, today);

        assertEquals(1, result.repetitionCount());
        assertTrue(result.stability() > 0, "Stability should be positive");
        assertTrue(result.difficulty() >= 1 && result.difficulty() <= 10, "Difficulty should be 1-10");
        assertTrue(result.intervalDays() >= 1, "Interval should be at least 1");
        assertNotNull(result.nextReviewDate());
    }

    @Test
    void newCard_againRating_setsShortInterval() {
        SchedulingResult result = fsrsService.calculate(0, 0, 0, 0, 0, today);

        assertEquals(1, result.repetitionCount());
        assertEquals(1, result.intervalDays());
    }

    @Test
    void secondReview_goodRating_increasesStability() {
        // First review
        SchedulingResult first = fsrsService.calculate(0, 0, 0, 4, 0, today);

        // Second review after the interval
        LocalDate reviewDate = today.plusDays(first.intervalDays());
        SchedulingResult second = fsrsService.calculate(
                first.stability(), first.difficulty(), first.intervalDays(),
                4, first.repetitionCount(), reviewDate);

        assertTrue(second.stability() > first.stability(),
                "Stability should increase on successive good reviews");
        assertEquals(2, second.repetitionCount());
    }

    @Test
    void failedReview_resetsReps_decreasesStability() {
        // Build up some stability
        SchedulingResult good = fsrsService.calculate(0, 0, 0, 5, 0, today);
        LocalDate day2 = today.plusDays(good.intervalDays());
        SchedulingResult built = fsrsService.calculate(
                good.stability(), good.difficulty(), good.intervalDays(),
                5, good.repetitionCount(), day2);

        // Now fail
        LocalDate day3 = day2.plusDays(built.intervalDays());
        SchedulingResult failed = fsrsService.calculate(
                built.stability(), built.difficulty(), built.intervalDays(),
                0, built.repetitionCount(), day3);

        assertEquals(0, failed.repetitionCount());
        assertTrue(failed.stability() <= built.stability(),
                "Stability should not increase after failure");
    }

    @Test
    void easyRating_givesLongerInterval_thanGoodRating() {
        SchedulingResult good = fsrsService.calculate(0, 0, 0, 4, 0, today);
        SchedulingResult easy = fsrsService.calculate(0, 0, 0, 5, 0, today);

        assertTrue(easy.intervalDays() >= good.intervalDays(),
                "Easy rating should give equal or longer interval than good");
    }

    @Test
    void stabilityNeverBelowMinimum() {
        // Multiple failures shouldn't crash or go below minimum
        SchedulingResult result = fsrsService.calculate(0.1, 10, 1, 0, 1, today);

        assertTrue(result.stability() >= 0.1, "Stability should never go below 0.1");
    }

    @Test
    void difficultyClampedTo1_10() {
        // Easy rating on new card should have difficulty >= 1
        SchedulingResult easy = fsrsService.calculate(0, 0, 0, 5, 0, today);
        assertTrue(easy.difficulty() >= 1 && easy.difficulty() <= 10);

        // Fail rating should have difficulty >= 1
        SchedulingResult fail = fsrsService.calculate(0, 0, 0, 0, 0, today);
        assertTrue(fail.difficulty() >= 1 && fail.difficulty() <= 10);
    }

    @Test
    void easeFactorComputed_forCompatibility() {
        SchedulingResult result = fsrsService.calculate(0, 0, 0, 4, 0, today);

        // Ease factor should be >= 1.3 (minimum from SM-2 compatibility)
        assertTrue(result.easeFactor() >= 1.3);
    }
}
