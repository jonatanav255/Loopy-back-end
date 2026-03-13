package com.loopy.card.dto;

// Dependencies: @NotBlank, @NotNull — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull UUID conceptId,
        @NotBlank String front,
        @NotBlank String back,
        CardType cardType,
        String hint,
        String sourceUrl
) {}
