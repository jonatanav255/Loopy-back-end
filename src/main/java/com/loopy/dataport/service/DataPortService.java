package com.loopy.dataport.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.dataport.dto.*;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DataPortService {

    private static final String EXPORT_VERSION = "1.0";

    private final TopicRepository topicRepository;
    private final ConceptRepository conceptRepository;
    private final CardRepository cardRepository;

    public DataPortService(TopicRepository topicRepository,
                           ConceptRepository conceptRepository,
                           CardRepository cardRepository) {
        this.topicRepository = topicRepository;
        this.conceptRepository = conceptRepository;
        this.cardRepository = cardRepository;
    }

    /**
     * Exports user data as a nested JSON structure.
     * If topicIds is provided, only those topics are exported; otherwise all topics.
     */
    @Transactional(readOnly = true)
    public ExportResponse exportData(User user, List<UUID> topicIds) {
        List<Topic> topics;
        if (topicIds != null && !topicIds.isEmpty()) {
            topics = new ArrayList<>();
            for (UUID topicId : topicIds) {
                Topic topic = topicRepository.findByIdAndUserId(topicId, user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
                topics.add(topic);
            }
        } else {
            topics = topicRepository.findByUserIdOrderBySortOrderAsc(user.getId());
        }

        int totalConcepts = 0;
        int totalCards = 0;
        List<ExportTopicData> topicDataList = new ArrayList<>();

        for (Topic topic : topics) {
            List<Concept> concepts = conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(
                    topic.getId(), user.getId());

            List<ExportConceptData> conceptDataList = new ArrayList<>();
            for (Concept concept : concepts) {
                List<Card> cards = cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(
                        concept.getId(), user.getId());

                List<ExportCardData> cardDataList = cards.stream()
                        .map(ExportCardData::from)
                        .toList();

                conceptDataList.add(ExportConceptData.from(concept, cardDataList));
                totalCards += cards.size();
            }

            topicDataList.add(ExportTopicData.from(topic, conceptDataList));
            totalConcepts += concepts.size();
        }

        return new ExportResponse(
                EXPORT_VERSION,
                Instant.now(),
                topics.size(),
                totalConcepts,
                totalCards,
                topicDataList
        );
    }

    /**
     * Imports data from an export payload, creating new entities for the importing user.
     * Scheduling state is reset to defaults (fresh start).
     */
    @Transactional
    public ImportResponse importData(ImportRequest request, User user) {
        int topicsCreated = 0;
        int conceptsCreated = 0;
        int cardsCreated = 0;

        for (ImportTopicData topicData : request.topics()) {
            Topic topic = new Topic(
                    user,
                    topicData.name(),
                    topicData.description(),
                    topicData.colorHex() != null ? topicData.colorHex() : "#6366F1"
            );
            topic = topicRepository.save(topic);
            topicsCreated++;

            if (topicData.concepts() == null) continue;

            for (ImportConceptData conceptData : topicData.concepts()) {
                Concept concept = new Concept(topic, user, conceptData.title(), conceptData.notes());
                if (conceptData.referenceExplanation() != null) {
                    concept.setReferenceExplanation(conceptData.referenceExplanation());
                }
                concept = conceptRepository.save(concept);
                conceptsCreated++;

                if (conceptData.cards() == null) continue;

                for (ImportCardData cardData : conceptData.cards()) {
                    CardType cardType = CardType.STANDARD;
                    if (cardData.cardType() != null) {
                        try {
                            cardType = CardType.valueOf(cardData.cardType());
                        } catch (IllegalArgumentException ignored) {
                            // Fall back to STANDARD for unrecognized types
                        }
                    }

                    Card card = new Card(
                            concept,
                            user,
                            cardData.front(),
                            cardData.back(),
                            cardType,
                            cardData.hint(),
                            cardData.sourceUrl()
                    );
                    cardRepository.save(card);
                    cardsCreated++;
                }
            }
        }

        return new ImportResponse(topicsCreated, conceptsCreated, cardsCreated);
    }
}
