package com.loopy.teachback.dto;

// Dependencies: @NotNull, @NotBlank, @Min, @Max — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SubmitTeachBackRequest(
        @NotNull UUID conceptId,
        @NotBlank String userExplanation,
        @NotNull @Min(1) @Max(5) Integer selfRating,
        List<String> gapsFound
) {}
