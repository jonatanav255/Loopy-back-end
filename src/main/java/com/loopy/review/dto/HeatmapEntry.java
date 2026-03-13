package com.loopy.review.dto;

import java.time.LocalDate;

/**
 * A single day's review count for the activity heatmap.
 */
public record HeatmapEntry(
        LocalDate date,
        long count
) {}
