package com.loopy.auth.dto;

// Dependencies: record — see DEPENDENCY_GUIDE.md
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
