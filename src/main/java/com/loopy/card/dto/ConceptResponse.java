package com.loopy.card.dto;

import com.loopy.card.entity.Concept;

import java.time.Instant;
import java.util.UUID;

public record ConceptResponse(
        UUID id,
        UUID topicId,
        String title,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConceptResponse from(Concept concept) {
        return new ConceptResponse(
                concept.getId(),
                concept.getTopic().getId(),
                concept.getTitle(),
                concept.getNotes(),
                concept.getStatus().name(),
                concept.getCreatedAt(),
                concept.getUpdatedAt()
        );
    }
}
