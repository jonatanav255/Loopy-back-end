package com.loopy.dataport.dto;

import com.loopy.topic.entity.Topic;

import java.util.List;

/**
 * Topic data within an export — includes nested concepts.
 */
public record ExportTopicData(
        String name,
        String description,
        String colorHex,
        List<ExportConceptData> concepts
) {
    public static ExportTopicData from(Topic topic, List<ExportConceptData> concepts) {
        return new ExportTopicData(
                topic.getName(),
                topic.getDescription(),
                topic.getColorHex(),
                concepts
        );
    }
}
