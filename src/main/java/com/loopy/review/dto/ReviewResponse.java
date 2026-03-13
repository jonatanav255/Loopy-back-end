package com.loopy.review.dto;

import com.loopy.card.dto.CardResponse;
import com.loopy.card.entity.Card;
import com.loopy.review.entity.ReviewLog;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID reviewLogId,
        int rating,
        Long responseTimeMs,
        Instant reviewedAt,
        CardResponse updatedCard
) {
    public static ReviewResponse from(ReviewLog log, Card card) {
        return new ReviewResponse(
                log.getId(),
                log.getRating(),
                log.getResponseTimeMs(),
                log.getReviewedAt(),
                CardResponse.from(card)
        );
    }
}
