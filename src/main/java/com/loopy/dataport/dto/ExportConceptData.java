package com.loopy.dataport.dto;

import com.loopy.card.entity.Concept;

import java.util.List;

/**
 * Concept data within an export — includes nested cards.
 */
public record ExportConceptData(
        String title,
        String notes,
        String referenceExplanation,
        List<ExportCardData> cards
) {
    public static ExportConceptData from(Concept concept, List<ExportCardData> cards) {
        return new ExportConceptData(
                concept.getTitle(),
                concept.getNotes(),
                concept.getReferenceExplanation(),
                cards
        );
    }
}
