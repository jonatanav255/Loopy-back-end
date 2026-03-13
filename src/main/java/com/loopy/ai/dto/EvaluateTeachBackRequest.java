package com.loopy.ai.dto;

// Dependencies: @NotNull, @NotBlank — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EvaluateTeachBackRequest(
        @NotNull UUID conceptId,
        @NotBlank String userExplanation
) {}
