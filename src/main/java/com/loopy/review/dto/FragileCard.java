package com.loopy.review.dto;

import com.loopy.card.dto.CardResponse;

/**
 * A card flagged as "fragile knowledge" — correct but low confidence.
 */
public record FragileCard(
        CardResponse card,
        int lastRating,
        int lastConfidence,
        long occurrences
) {}
