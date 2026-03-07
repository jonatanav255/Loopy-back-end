package com.loopy.auth.dto;

// Dependencies: record, @NotBlank, @Email, @Size — see DEPENDENCY_GUIDE.md
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password
) {}
