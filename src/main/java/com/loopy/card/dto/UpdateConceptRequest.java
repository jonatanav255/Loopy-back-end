package com.loopy.card.dto;

// Dependencies: @NotBlank, @Size — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConceptRequest(
        @NotBlank @Size(max = 200) String title,
        String notes
) {}
