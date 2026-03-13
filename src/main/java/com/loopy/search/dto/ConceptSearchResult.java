package com.loopy.search.dto;

import com.loopy.card.entity.Concept;

import java.util.UUID;

public record ConceptSearchResult(
        UUID id,
        String title,
        String notes,
        String status,
        UUID topicId,
        String topicName
) {
    public static ConceptSearchResult from(Concept concept) {
        return new ConceptSearchResult(
                concept.getId(),
                concept.getTitle(),
                concept.getNotes(),
                concept.getStatus().name(),
                concept.getTopic().getId(),
                concept.getTopic().getName()
        );
    }
}
