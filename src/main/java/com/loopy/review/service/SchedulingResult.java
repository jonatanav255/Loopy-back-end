package com.loopy.review.service;

import java.time.LocalDate;

/**
 * Unified scheduling result from either SM-2 or FSRS algorithm.
 */
public record SchedulingResult(
        int repetitionCount,
        double easeFactor,
        int intervalDays,
        LocalDate nextReviewDate,
        double stability,
        double difficulty
) {}
