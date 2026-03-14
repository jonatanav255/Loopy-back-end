package com.loopy.search.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.search.dto.CardSearchResult;
import com.loopy.search.dto.ConceptSearchResult;
import com.loopy.search.dto.SearchResponse;
import com.loopy.search.dto.TopicSearchResult;
import com.loopy.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchService {

    private static final int MAX_RESULTS_PER_TYPE = 10;

    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final CardRepository cardRepository;

    public SearchService(TopicRepository topicRepository,
                         ConceptRepository conceptRepository,
                         CardRepository cardRepository) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, User user) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = "%" + escaped + "%";

        List<TopicSearchResult> topics = topicRepository
                .searchByUser(user.getId(), pattern)
                .stream()
                .limit(MAX_RESULTS_PER_TYPE)
                .map(TopicSearchResult::from)
                .toList();

        List<ConceptSearchResult> concepts = conceptRepository
                .searchByUser(user.getId(), pattern)
                .stream()
                .limit(MAX_RESULTS_PER_TYPE)
                .map(ConceptSearchResult::from)
                .toList();

        List<CardSearchResult> cards = cardRepository
                .searchByUser(user.getId(), pattern)
                .stream()
                .limit(MAX_RESULTS_PER_TYPE)
                .map(CardSearchResult::from)
                .toList();

        return new SearchResponse(topics, concepts, cards);
    }
}
