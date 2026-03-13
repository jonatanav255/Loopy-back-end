package com.loopy.ai.dto;

// Dependencies: @NotNull, @NotBlank, @Min, @Max — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateCardsRequest(
        @NotNull UUID conceptId,
        @NotBlank String content,
        @Min(1) @Max(10) int numCards
) {}
