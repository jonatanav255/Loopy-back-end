package com.loopy.review.service;

import java.time.LocalDate;

/**
 * Result of an SM-2 calculation — new scheduling state for a card after review.
 */
public record SM2Result(
        int repetitionCount,
        double easeFactor,
        int intervalDays,
        LocalDate nextReviewDate
) {}
