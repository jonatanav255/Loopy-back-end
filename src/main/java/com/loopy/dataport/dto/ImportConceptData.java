package com.loopy.dataport.dto;

// Dependencies: @NotBlank, @Size, @Valid — see DEPENDENCY_GUIDE.md
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImportConceptData(
        @NotBlank(message = "Concept title is required")
        @Size(max = 200)
        String title,

        String notes,

        String referenceExplanation,

        List<@Valid ImportCardData> cards
) {}
