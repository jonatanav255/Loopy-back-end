package com.loopy.dataport.dto;

/**
 * Summary returned after a successful import.
 */
public record ImportResponse(
        int topicsCreated,
        int conceptsCreated,
        int cardsCreated
) {}
