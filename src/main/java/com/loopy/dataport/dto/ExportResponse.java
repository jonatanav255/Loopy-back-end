package com.loopy.dataport.dto;

import java.time.Instant;
import java.util.List;

/**
 * Top-level export payload — includes metadata and nested topic data.
 */
public record ExportResponse(
        String exportVersion,
        Instant exportedAt,
        int topicCount,
        int conceptCount,
        int cardCount,
        List<ExportTopicData> topics
) {}
