package com.loopy.dataport.dto;

// Dependencies: @NotBlank — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;

public record ImportCardData(
        @NotBlank(message = "Card front is required")
        String front,

        @NotBlank(message = "Card back is required")
        String back,

        String cardType,

        String hint,

        String sourceUrl
) {}
