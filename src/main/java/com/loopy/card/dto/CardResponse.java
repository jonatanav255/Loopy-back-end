package com.loopy.card.dto;

import com.loopy.card.entity.Card;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID conceptId,
        String front,
        String back,
        String cardType,
        String hint,
        String sourceUrl,
        int repetitionCount,
        double easeFactor,
        int intervalDays,
        LocalDate nextReviewDate,
        LocalDate lastReviewDate,
        double stability,
        double difficulty,
        String schedulingAlgorithm,
        Instant createdAt,
        Instant updatedAt
) {
    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getConcept().getId(),
                card.getFront(),
                card.getBack(),
                card.getCardType().name(),
                card.getHint(),
                card.getSourceUrl(),
                card.getRepetitionCount(),
                card.getEaseFactor(),
                card.getIntervalDays(),
                card.getNextReviewDate(),
                card.getLastReviewDate(),
                card.getStability(),
                card.getDifficulty(),
                card.getSchedulingAlgorithm().name(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
