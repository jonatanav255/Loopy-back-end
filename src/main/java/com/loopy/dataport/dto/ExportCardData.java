package com.loopy.dataport.dto;

import com.loopy.card.entity.Card;

/**
 * Card data within an export — content only, no scheduling state or IDs.
 */
public record ExportCardData(
        String front,
        String back,
        String cardType,
        String hint,
        String sourceUrl
) {
    public static ExportCardData from(Card card) {
        return new ExportCardData(
                card.getFront(),
                card.getBack(),
                card.getCardType().name(),
                card.getHint(),
                card.getSourceUrl()
        );
    }
}
