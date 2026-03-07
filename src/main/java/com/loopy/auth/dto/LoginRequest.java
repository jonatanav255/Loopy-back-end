package com.loopy.auth.dto;

// Dependencies: record, @NotBlank, @Email — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
