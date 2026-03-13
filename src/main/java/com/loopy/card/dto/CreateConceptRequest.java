package com.loopy.card.dto;

// Dependencies: @NotBlank, @NotNull, @Size — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateConceptRequest(
        @NotNull UUID topicId,
        @NotBlank @Size(max = 200) String title,
        String notes
) {}
