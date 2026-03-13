package com.loopy.topic.dto;

import com.loopy.topic.entity.Topic;

import java.time.Instant;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        String name,
        String description,
        String colorHex,
        Instant createdAt,
        Instant updatedAt,
        long cardCount
) {
    public static TopicResponse from(Topic topic) {
        return from(topic, 0);
    }

    public static TopicResponse from(Topic topic, long cardCount) {
        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getColorHex(),
                topic.getCreatedAt(),
                topic.getUpdatedAt(),
                cardCount
        );
    }
}
