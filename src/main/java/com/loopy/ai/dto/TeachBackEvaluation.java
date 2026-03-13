package com.loopy.ai.dto;

import java.util.List;

/**
 * AI evaluation of a teach-back explanation.
 */
public record TeachBackEvaluation(
        int clarityScore,
        int accuracyScore,
        int completenessScore,
        String feedback,
        List<String> followUpQuestions,
        List<String> detectedGaps,
        List<GeneratedCard> suggestedCards
) {}
