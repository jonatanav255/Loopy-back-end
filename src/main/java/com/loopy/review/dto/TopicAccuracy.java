package com.loopy.review.dto;

import java.util.UUID;

/**
 * Accuracy stats for a single topic.
 */
public record TopicAccuracy(
        UUID topicId,
        String topicName,
        long totalReviews,
        long passedReviews,
        double accuracy
) {}
