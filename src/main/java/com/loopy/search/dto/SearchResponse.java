package com.loopy.search.dto;

import java.util.List;

public record SearchResponse(
        List<TopicSearchResult> topics,
        List<ConceptSearchResult> concepts,
        List<CardSearchResult> cards
) {
}
