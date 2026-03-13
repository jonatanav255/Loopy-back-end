package com.loopy.search.dto;

import com.loopy.card.entity.Card;

import java.util.UUID;

public record CardSearchResult(
        UUID id,
        String front,
        String back,
        String hint,
        String cardType,
        UUID conceptId,
        String conceptTitle,
        UUID topicId,
        String topicName
) {
    public static CardSearchResult from(Card card) {
        return new CardSearchResult(
                card.getId(),
                card.getFront(),
                card.getBack(),
                card.getHint(),
                card.getCardType().name(),
                card.getConcept().getId(),
                card.getConcept().getTitle(),
                card.getConcept().getTopic().getId(),
                card.getConcept().getTopic().getName()
        );
    }
}
