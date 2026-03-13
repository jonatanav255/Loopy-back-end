package com.loopy.review.dto;

/**
 * High-level overview stats for the user's dashboard.
 */
public record StatsOverview(
        int cardsDueToday,
        int cardsReviewedToday,
        int totalCards,
        double accuracyToday,
        int currentStreak,
        int longestStreak
) {}
