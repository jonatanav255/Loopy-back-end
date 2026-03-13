package com.loopy.review;

import com.loopy.review.service.SM2Result;
import com.loopy.review.service.SM2Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit test for SM-2 algorithm — no Spring context needed.
 */
class SM2ServiceTest {

    private SM2Service sm2Service;
    private final LocalDate today = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        sm2Service = new SM2Service();
    }

    @Test
    void firstReviewPass_setsIntervalTo1Day() {
        SM2Result result = sm2Service.calculate(0, 2.5, 0, 4, today);

        assertEquals(1, result.repetitionCount());
        assertEquals(1, result.intervalDays());
        assertEquals(today.plusDays(1), result.nextReviewDate());
    }

    @Test
    void secondReviewPass_setsIntervalTo6Days() {
        SM2Result result = sm2Service.calculate(1, 2.5, 1, 4, today);

        assertEquals(2, result.repetitionCount());
        assertEquals(6, result.intervalDays());
        assertEquals(today.plusDays(6), result.nextReviewDate());
    }

    @Test
    void thirdReviewPass_multipliesIntervalByEF() {
        SM2Result result = sm2Service.calculate(2, 2.5, 6, 4, today);

        assertEquals(3, result.repetitionCount());
        // EF' = 2.5 + (0.1 - (5-4)*(0.08 + (5-4)*0.02)) = 2.5 + (0.1 - 0.1) = 2.5
        // interval = round(6 * 2.5) = 15
        assertEquals(15, result.intervalDays());
        assertEquals(today.plusDays(15), result.nextReviewDate());
    }

    @Test
    void failedReview_resetsRepsAndInterval() {
        SM2Result result = sm2Service.calculate(5, 2.5, 30, 2, today);

        assertEquals(0, result.repetitionCount());
        assertEquals(1, result.intervalDays());
        assertEquals(today.plusDays(1), result.nextReviewDate());
    }

    @Test
    void easeFactorFloor_neverBelow1_3() {
        // Multiple bad ratings should floor at 1.3
        SM2Result result = sm2Service.calculate(0, 1.3, 0, 0, today);

        assertTrue(result.easeFactor() >= 1.3);
    }

    @Test
    void rating5_boostsEaseFactor() {
        SM2Result result = sm2Service.calculate(0, 2.5, 0, 5, today);

        // EF' = 2.5 + (0.1 - 0*(0.08 + 0*0.02)) = 2.5 + 0.1 = 2.6
        assertEquals(2.6, result.easeFactor(), 0.001);
    }

    @Test
    void rating3_passesButLowersEF() {
        SM2Result result = sm2Service.calculate(0, 2.5, 0, 3, today);

        // EF' = 2.5 + (0.1 - 2*(0.08 + 2*0.02)) = 2.5 + (0.1 - 0.24) = 2.36
        assertEquals(2.36, result.easeFactor(), 0.001);
        assertEquals(1, result.repetitionCount());
    }

    @Test
    void rating0_totalBlackout() {
        SM2Result result = sm2Service.calculate(3, 2.5, 15, 0, today);

        assertEquals(0, result.repetitionCount());
        assertEquals(1, result.intervalDays());
        // EF is lowered but floored at 1.3
        // EF' = 2.5 + (0.1 - 5*(0.08 + 5*0.02)) = 2.5 + (0.1 - 0.9) = 1.7
        assertEquals(1.7, result.easeFactor(), 0.001);
    }

    @Test
    void dateCalculation_correctForLargeInterval() {
        SM2Result result = sm2Service.calculate(5, 2.5, 60, 5, today);

        // EF' = 2.6, interval = round(60 * 2.6) = 156
        assertEquals(today.plusDays(156), result.nextReviewDate());
    }
}
