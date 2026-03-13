package com.loopy.card.dto;

// Dependencies: @NotBlank — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.CardType;
import jakarta.validation.constraints.NotBlank;

public record UpdateCardRequest(
        @NotBlank String front,
        @NotBlank String back,
        CardType cardType,
        String hint,
        String sourceUrl
) {}
