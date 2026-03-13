package com.loopy.teachback.dto;

import com.loopy.teachback.entity.TeachBack;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record TeachBackResponse(
        UUID id,
        UUID conceptId,
        String conceptTitle,
        String userExplanation,
        String referenceExplanation,
        int selfRating,
        List<String> gapsFound,
        Instant createdAt
) {
    public static TeachBackResponse from(TeachBack tb) {
        List<String> gaps = tb.getGapsFound() != null && !tb.getGapsFound().isBlank()
                ? Arrays.asList(tb.getGapsFound().split("\\|"))
                : List.of();

        return new TeachBackResponse(
                tb.getId(),
                tb.getConcept().getId(),
                tb.getConcept().getTitle(),
                tb.getUserExplanation(),
                tb.getConcept().getReferenceExplanation(),
                tb.getSelfRating(),
                gaps,
                tb.getCreatedAt()
        );
    }
}
