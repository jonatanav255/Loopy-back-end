package com.loopy.review.service;

// Dependencies: @Service — see DEPENDENCY_GUIDE.md
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Pure SM-2 algorithm implementation.
 * Takes current card state + rating, returns new scheduling state.
 * No database or Spring dependencies beyond @Service for DI.
 */
@Service
public class SM2Service {

    /**
     * Calculates the next SM-2 state after a review.
     *
     * @param repetitionCount current number of successful consecutive reviews
     * @param easeFactor      current ease factor (minimum 1.3)
     * @param intervalDays    current interval in days
     * @param rating          user rating 0-5 (0-2 = fail, 3-5 = pass)
     * @param today           current date (parameter for testability)
     * @return new SM-2 state with updated scheduling
     */
    public SM2Result calculate(int repetitionCount, double easeFactor, int intervalDays,
                               int rating, LocalDate today) {
        // Update ease factor: EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        double newEF = easeFactor + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02));
        newEF = Math.max(1.3, newEF);

        int newReps;
        int newInterval;

        if (rating < 3) {
            // Failed: reset to beginning
            newReps = 0;
            newInterval = 1;
        } else {
            // Passed
            newReps = repetitionCount + 1;
            if (newReps == 1) {
                newInterval = 1;
            } else if (newReps == 2) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.round(intervalDays * newEF);
            }
        }

        LocalDate nextReview = today.plusDays(newInterval);

        return new SM2Result(newReps, newEF, newInterval, nextReview);
    }
}
