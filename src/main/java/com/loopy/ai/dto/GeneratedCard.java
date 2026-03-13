package com.loopy.ai.dto;

/**
 * A card suggested by AI. The user reviews and confirms before saving.
 */
public record GeneratedCard(
        String front,
        String back,
        String cardType,
        String hint
) {}
