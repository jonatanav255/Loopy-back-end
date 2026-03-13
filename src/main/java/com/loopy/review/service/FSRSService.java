package com.loopy.review.service;

// Dependencies: @Service — see DEPENDENCY_GUIDE.md
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * FSRS-5 (Free Spaced Repetition Scheduler) algorithm implementation.
 *
 * Core concepts:
 * - Stability (S): how many days until recall probability drops to 90%
 * - Difficulty (D): card difficulty on a 1-10 scale
 * - Retrievability (R): probability of recall at the time of review
 *
 * Reference: https://github.com/open-spaced-repetition/fsrs4anki
 */
@Service
public class FSRSService {

    // Default FSRS-5 parameters (can be personalized via training later)
    private static final double[] W = {
            0.4072, 1.1829, 3.1262, 15.4722, // w0-w3: initial stability for ratings 1-4
            7.2102, 0.5316, 1.0651,           // w4-w6: difficulty parameters
            0.0092, 1.5247, 0.1175, 0.9507,   // w7-w10: stability after success
            2.9898, 0.0922, 0.2907, 1.4820,   // w11-w14: stability after failure
            0.2195, 2.8237,                    // w15-w16: short-term stability
            0.0, 0.0                           // w17-w18: reserved
    };

    private static final double DECAY = -0.5;
    private static final double FACTOR = Math.pow(0.9, 1.0 / DECAY) - 1;
    private static final double DESIRED_RETENTION = 0.9;

    /**
     * Calculates the next FSRS state after a review.
     *
     * @param stability       current stability (days until 90% recall). 0 for new cards.
     * @param difficulty      current difficulty (1-10). 0 for new cards.
     * @param intervalDays    days since last review
     * @param rating          user rating 1-4 (1=Again, 2=Hard, 3=Good, 4=Easy)
     * @param repetitionCount current repetition count
     * @param today           current date
     * @return new scheduling state
     */
    public SchedulingResult calculate(double stability, double difficulty, int intervalDays,
                                      int rating, int repetitionCount, LocalDate today) {
        // Convert 0-5 SM-2 rating to 1-4 FSRS rating
        int fsrsRating = convertRating(rating);

        double newStability;
        double newDifficulty;
        int newReps;

        if (repetitionCount == 0 || stability == 0) {
            // First review (new card)
            newStability = initialStability(fsrsRating);
            newDifficulty = initialDifficulty(fsrsRating);
            newReps = 1;
        } else {
            // Subsequent reviews
            double retrievability = calculateRetrievability(intervalDays, stability);
            newDifficulty = nextDifficulty(difficulty, fsrsRating);

            if (fsrsRating == 1) {
                // Failed: use forgetting stability
                newStability = nextForgetStability(newDifficulty, stability, retrievability);
                newReps = 0;
            } else {
                // Passed: use recall stability
                newStability = nextRecallStability(newDifficulty, stability, retrievability, fsrsRating);
                newReps = repetitionCount + 1;
            }
        }

        int newInterval = nextInterval(newStability);
        LocalDate nextReview = today.plusDays(newInterval);

        // Map FSRS difficulty back to an easeFactor-like value for compatibility
        double easeFactor = 2.5 + (5.5 - newDifficulty) * 0.2;
        easeFactor = Math.max(1.3, easeFactor);

        return new SchedulingResult(newReps, easeFactor, newInterval, nextReview,
                newStability, newDifficulty);
    }

    /** Initial stability for a new card based on first rating. */
    private double initialStability(int rating) {
        return Math.max(0.1, W[rating - 1]);
    }

    /** Initial difficulty for a new card based on first rating. */
    private double initialDifficulty(int rating) {
        double d = W[4] - Math.exp(W[5] * (rating - 1)) + 1;
        return clampDifficulty(d);
    }

    /** Retrievability: probability of recall after `elapsedDays` at given stability. */
    private double calculateRetrievability(int elapsedDays, double stability) {
        if (stability <= 0) return 0;
        return Math.pow(1 + FACTOR * elapsedDays / stability, DECAY);
    }

    /** Next difficulty after a review. */
    private double nextDifficulty(double difficulty, int rating) {
        double deltaDifficulty = W[6] * (rating - 3);
        double d = difficulty - deltaDifficulty;
        // Mean reversion towards initial difficulty
        double dInitial = initialDifficulty(4);
        d = W[7] * dInitial + (1 - W[7]) * d;
        return clampDifficulty(d);
    }

    /** Stability after successful recall (rating >= 2). */
    private double nextRecallStability(double difficulty, double stability,
                                       double retrievability, int rating) {
        double hardPenalty = (rating == 2) ? W[15] : 1.0;
        double easyBonus = (rating == 4) ? W[16] : 1.0;

        double newS = stability * (1 + Math.exp(W[8])
                * (11 - difficulty)
                * Math.pow(stability, -W[9])
                * (Math.exp((1 - retrievability) * W[10]) - 1)
                * hardPenalty * easyBonus);

        return Math.max(0.1, newS);
    }

    /** Stability after forgetting (rating == 1). */
    private double nextForgetStability(double difficulty, double stability,
                                       double retrievability) {
        double newS = W[11]
                * Math.pow(difficulty, -W[12])
                * (Math.pow(stability + 1, W[13]) - 1)
                * Math.exp((1 - retrievability) * W[14]);

        return Math.max(0.1, Math.min(newS, stability));
    }

    /** Calculate interval from stability targeting desired retention. */
    private int nextInterval(double stability) {
        double interval = stability / FACTOR * (Math.pow(DESIRED_RETENTION, 1.0 / DECAY) - 1);
        return Math.max(1, (int) Math.round(interval));
    }

    /** Clamp difficulty to [1, 10]. */
    private double clampDifficulty(double d) {
        return Math.max(1, Math.min(10, d));
    }

    /**
     * Convert SM-2 rating (0-5) to FSRS rating (1-4).
     * 0-1 → 1 (Again), 2 → 2 (Hard), 3-4 → 3 (Good), 5 → 4 (Easy)
     */
    private int convertRating(int sm2Rating) {
        if (sm2Rating <= 1) return 1;
        if (sm2Rating == 2) return 2;
        if (sm2Rating <= 4) return 3;
        return 4;
    }
}
