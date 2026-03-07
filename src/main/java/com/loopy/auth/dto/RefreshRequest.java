package com.loopy.auth.dto;

// Dependencies: record, @NotBlank — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
) {}
