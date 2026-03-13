package com.loopy.review.dto;

// Dependencies: @NotNull, @Min, @Max — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(
        @NotNull @Min(0) @Max(5) Integer rating,
        Long responseTimeMs
) {}
