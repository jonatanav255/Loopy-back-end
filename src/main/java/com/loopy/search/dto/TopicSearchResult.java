package com.loopy.search.dto;

import com.loopy.topic.entity.Topic;

import java.util.UUID;

public record TopicSearchResult(
        UUID id,
        String name,
        String description,
        String colorHex
) {
    public static TopicSearchResult from(Topic topic) {
        return new TopicSearchResult(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getColorHex()
        );
    }
}
