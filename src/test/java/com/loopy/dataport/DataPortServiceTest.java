package com.loopy.dataport;

import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.dataport.dto.*;
import com.loopy.dataport.service.DataPortService;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataPortService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class DataPortServiceTest {

    @Mock private TopicRepository topicRepository;
    @Mock private ConceptRepository conceptRepository;
    @Mock private CardRepository cardRepository;

    private DataPortService dataPortService;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        dataPortService = new DataPortService(topicRepository, conceptRepository, cardRepository);
        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);
    }

    // ── Export: basic structure ──

    @Test
    void exportData_allTopics_returnsNestedStructure() {
        Topic topic = new Topic(testUser, "Java", "Learn Java", "#FF0000");
        setId(topic, UUID.randomUUID());

        Concept concept = new Concept(topic, testUser, "OOP", "Object oriented");
        setId(concept, UUID.randomUUID());

        Card card = new Card(concept, testUser, "What is OOP?", "A paradigm", CardType.STANDARD, null, null);
        setId(card, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(List.of(card));

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals("1.0", response.exportVersion());
        assertNotNull(response.exportedAt());
        assertEquals(1, response.topicCount());
        assertEquals(1, response.conceptCount());
        assertEquals(1, response.cardCount());
        assertEquals(1, response.topics().size());

        ExportTopicData topicData = response.topics().get(0);
        assertEquals("Java", topicData.name());
        assertEquals("Learn Java", topicData.description());
        assertEquals("#FF0000", topicData.colorHex());
        assertEquals(1, topicData.concepts().size());

        ExportConceptData conceptData = topicData.concepts().get(0);
        assertEquals("OOP", conceptData.title());
        assertEquals("Object oriented", conceptData.notes());
        assertEquals(1, conceptData.cards().size());

        ExportCardData cardData = conceptData.cards().get(0);
        assertEquals("What is OOP?", cardData.front());
        assertEquals("A paradigm", cardData.back());
        assertEquals("STANDARD", cardData.cardType());
        assertNull(cardData.hint());
        assertNull(cardData.sourceUrl());
    }

    @Test
    void exportData_preservesAllCardFields() {
        Topic topic = new Topic(testUser, "T", null, "#000000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "C", null);
        setId(concept, UUID.randomUUID());
        Card card = new Card(concept, testUser, "Front", "Back",
                CardType.CODE_OUTPUT, "A hint", "https://example.com");
        setId(card, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(List.of(card));

        ExportResponse response = dataPortService.exportData(testUser, null);

        ExportCardData cardData = response.topics().get(0).concepts().get(0).cards().get(0);
        assertEquals("Front", cardData.front());
        assertEquals("Back", cardData.back());
        assertEquals("CODE_OUTPUT", cardData.cardType());
        assertEquals("A hint", cardData.hint());
        assertEquals("https://example.com", cardData.sourceUrl());
    }

    @Test
    void exportData_preservesConceptReferenceExplanation() {
        Topic topic = new Topic(testUser, "T", null, "#000000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "C", "Notes here");
        concept.setReferenceExplanation("Detailed ref explanation");
        setId(concept, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        ExportConceptData conceptData = response.topics().get(0).concepts().get(0);
        assertEquals("Notes here", conceptData.notes());
        assertEquals("Detailed ref explanation", conceptData.referenceExplanation());
    }

    @Test
    void exportData_conceptWithNullFields_exportsNulls() {
        Topic topic = new Topic(testUser, "T", null, "#000000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "C", null);
        setId(concept, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        ExportConceptData conceptData = response.topics().get(0).concepts().get(0);
        assertNull(conceptData.notes());
        assertNull(conceptData.referenceExplanation());
    }

    // ── Export: filtering ──

    @Test
    void exportData_specificTopics_onlyExportsRequested() {
        UUID topicId = UUID.randomUUID();
        Topic topic = new Topic(testUser, "Python", "Learn Python", "#00FF00");
        setId(topic, topicId);

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topicId, userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, List.of(topicId));

        assertEquals(1, response.topicCount());
        assertEquals("Python", response.topics().get(0).name());
        verify(topicRepository, never()).findByUserIdOrderBySortOrderAsc(any());
    }

    @Test
    void exportData_multipleSpecificTopics_exportsAll() {
        UUID topicId1 = UUID.randomUUID();
        UUID topicId2 = UUID.randomUUID();
        Topic topic1 = new Topic(testUser, "Topic A", null, "#111111");
        Topic topic2 = new Topic(testUser, "Topic B", null, "#222222");
        setId(topic1, topicId1);
        setId(topic2, topicId2);

        when(topicRepository.findByIdAndUserId(topicId1, userId)).thenReturn(Optional.of(topic1));
        when(topicRepository.findByIdAndUserId(topicId2, userId)).thenReturn(Optional.of(topic2));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(any(), eq(userId)))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, List.of(topicId1, topicId2));

        assertEquals(2, response.topicCount());
        assertEquals("Topic A", response.topics().get(0).name());
        assertEquals("Topic B", response.topics().get(1).name());
    }

    @Test
    void exportData_emptyTopicIdsList_treatedAsExportAll() {
        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, Collections.emptyList());

        assertEquals(0, response.topicCount());
        verify(topicRepository).findByUserIdOrderBySortOrderAsc(userId);
        verify(topicRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void exportData_topicNotFound_throwsException() {
        UUID badId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(badId, userId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> dataPortService.exportData(testUser, List.of(badId)));
        assertTrue(ex.getMessage().contains(badId.toString()));
    }

    // ── Export: edge cases ──

    @Test
    void exportData_emptyAccount_returnsEmptyExport() {
        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals("1.0", response.exportVersion());
        assertNotNull(response.exportedAt());
        assertEquals(0, response.topicCount());
        assertEquals(0, response.conceptCount());
        assertEquals(0, response.cardCount());
        assertTrue(response.topics().isEmpty());
    }

    @Test
    void exportData_topicWithConceptsButNoCards_countsCorrectly() {
        Topic topic = new Topic(testUser, "T", null, "#000000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "C", null);
        setId(concept, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals(1, response.topicCount());
        assertEquals(1, response.conceptCount());
        assertEquals(0, response.cardCount());
        assertTrue(response.topics().get(0).concepts().get(0).cards().isEmpty());
    }

    @Test
    void exportData_topicWithNoConcepts_returnsEmptyConceptList() {
        Topic topic = new Topic(testUser, "Empty Topic", null, "#000000");
        setId(topic, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals(1, response.topicCount());
        assertEquals(0, response.conceptCount());
        assertEquals(0, response.cardCount());
        assertTrue(response.topics().get(0).concepts().isEmpty());
    }

    @Test
    void exportData_multipleTopics_countsAcrossAll() {
        Topic topic1 = new Topic(testUser, "T1", null, "#111");
        Topic topic2 = new Topic(testUser, "T2", null, "#222");
        setId(topic1, UUID.randomUUID());
        setId(topic2, UUID.randomUUID());

        Concept c1 = new Concept(topic1, testUser, "C1", null);
        Concept c2 = new Concept(topic2, testUser, "C2", null);
        Concept c3 = new Concept(topic2, testUser, "C3", null);
        setId(c1, UUID.randomUUID());
        setId(c2, UUID.randomUUID());
        setId(c3, UUID.randomUUID());

        Card card1 = new Card(c1, testUser, "Q1", "A1", CardType.STANDARD, null, null);
        Card card2 = new Card(c2, testUser, "Q2", "A2", CardType.FILL_BLANK, null, null);
        Card card3 = new Card(c2, testUser, "Q3", "A3", CardType.COMPARE, null, null);
        setId(card1, UUID.randomUUID());
        setId(card2, UUID.randomUUID());
        setId(card3, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic1, topic2));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic1.getId(), userId))
                .thenReturn(List.of(c1));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic2.getId(), userId))
                .thenReturn(List.of(c2, c3));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(c1.getId(), userId))
                .thenReturn(List.of(card1));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(c2.getId(), userId))
                .thenReturn(List.of(card2, card3));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(c3.getId(), userId))
                .thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals(2, response.topicCount());
        assertEquals(3, response.conceptCount());
        assertEquals(3, response.cardCount());
        assertEquals(1, response.topics().get(0).concepts().size());
        assertEquals(2, response.topics().get(1).concepts().size());
    }

    @Test
    void exportData_allCardTypes_exportedCorrectly() {
        Topic topic = new Topic(testUser, "T", null, "#000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "C", null);
        setId(concept, UUID.randomUUID());

        List<Card> cards = List.of(
                makeCard(concept, "Q1", "A1", CardType.STANDARD),
                makeCard(concept, "Q2", "A2", CardType.CODE_OUTPUT),
                makeCard(concept, "Q3", "A3", CardType.SPOT_THE_BUG),
                makeCard(concept, "Q4", "A4", CardType.FILL_BLANK),
                makeCard(concept, "Q5", "A5", CardType.EXPLAIN_WHEN),
                makeCard(concept, "Q6", "A6", CardType.COMPARE)
        );

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic.getId(), userId))
                .thenReturn(List.of(concept));
        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept.getId(), userId))
                .thenReturn(cards);

        ExportResponse response = dataPortService.exportData(testUser, null);

        List<ExportCardData> exportedCards = response.topics().get(0).concepts().get(0).cards();
        assertEquals(6, exportedCards.size());
        assertEquals("STANDARD", exportedCards.get(0).cardType());
        assertEquals("CODE_OUTPUT", exportedCards.get(1).cardType());
        assertEquals("SPOT_THE_BUG", exportedCards.get(2).cardType());
        assertEquals("FILL_BLANK", exportedCards.get(3).cardType());
        assertEquals("EXPLAIN_WHEN", exportedCards.get(4).cardType());
        assertEquals("COMPARE", exportedCards.get(5).cardType());
    }

    // ── Import: entity creation ──

    @Test
    void importData_createsAllEntities() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Java", "Learn Java", "#FF0000", List.of(
                        new ImportConceptData("OOP", "Object oriented", null, List.of(
                                new ImportCardData("What is OOP?", "A paradigm", "STANDARD", null, null),
                                new ImportCardData("What is inheritance?", "A mechanism", "STANDARD", "Think about parents", null)
                        ))
                ))
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(1, response.conceptsCreated());
        assertEquals(2, response.cardsCreated());

        verify(topicRepository).save(any(Topic.class));
        verify(conceptRepository).save(any(Concept.class));
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void importData_entitiesOwnedByImportingUser() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", null, null, List.of(
                        new ImportConceptData("Concept", null, null, List.of(
                                new ImportCardData("Q", "A", null, null, null)
                        ))
                ))
        ));

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);

        when(topicRepository.save(topicCaptor.capture())).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(conceptCaptor.capture())).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        assertEquals(testUser, topicCaptor.getValue().getUser());
        assertEquals(testUser, conceptCaptor.getValue().getUser());
        assertEquals(testUser, cardCaptor.getValue().getUser());
    }

    @Test
    void importData_cardsGetDefaultSchedulingState() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, List.of(
                                new ImportCardData("Q", "A", null, null, null)
                        ))
                ))
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        Card savedCard = cardCaptor.getValue();
        assertEquals(0, savedCard.getRepetitionCount());
        assertEquals(2.5, savedCard.getEaseFactor());
        assertEquals(0, savedCard.getIntervalDays());
        assertNull(savedCard.getLastReviewDate());
    }

    @Test
    void importData_preservesHintAndSourceUrl() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, List.of(
                                new ImportCardData("Q", "A", "STANDARD", "My hint", "https://source.com")
                        ))
                ))
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        Card savedCard = cardCaptor.getValue();
        assertEquals("My hint", savedCard.getHint());
        assertEquals("https://source.com", savedCard.getSourceUrl());
    }

    // ── Import: card types ──

    @Test
    void importData_validCardTypes_parsedCorrectly() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, List.of(
                                new ImportCardData("Q1", "A1", "CODE_OUTPUT", null, null),
                                new ImportCardData("Q2", "A2", "SPOT_THE_BUG", null, null),
                                new ImportCardData("Q3", "A3", "FILL_BLANK", null, null),
                                new ImportCardData("Q4", "A4", "EXPLAIN_WHEN", null, null),
                                new ImportCardData("Q5", "A5", "COMPARE", null, null)
                        ))
                ))
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        List<Card> saved = cardCaptor.getAllValues();
        assertEquals(CardType.CODE_OUTPUT, saved.get(0).getCardType());
        assertEquals(CardType.SPOT_THE_BUG, saved.get(1).getCardType());
        assertEquals(CardType.FILL_BLANK, saved.get(2).getCardType());
        assertEquals(CardType.EXPLAIN_WHEN, saved.get(3).getCardType());
        assertEquals(CardType.COMPARE, saved.get(4).getCardType());
    }

    @Test
    void importData_nullCardType_defaultsToStandard() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, List.of(
                                new ImportCardData("Q", "A", null, null, null)
                        ))
                ))
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        assertEquals(CardType.STANDARD, cardCaptor.getValue().getCardType());
    }

    @Test
    void importData_unknownCardType_fallsBackToStandard() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, List.of(
                                new ImportCardData("Q", "A", "NONEXISTENT_TYPE", null, null)
                        ))
                ))
        ));

        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        when(cardRepository.save(cardCaptor.capture())).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.cardsCreated());
        assertEquals(CardType.STANDARD, cardCaptor.getValue().getCardType());
    }

    // ── Import: default values ──

    @Test
    void importData_defaultColor_whenNull() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("No Color", "Desc", null, null)
        ));

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        when(topicRepository.save(topicCaptor.capture())).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

        dataPortService.importData(request, testUser);

        assertEquals("#6366F1", topicCaptor.getValue().getColorHex());
    }

    @Test
    void importData_customColor_preserved() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Colorful", null, "#ABCDEF", null)
        ));

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        when(topicRepository.save(topicCaptor.capture())).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

        dataPortService.importData(request, testUser);

        assertEquals("#ABCDEF", topicCaptor.getValue().getColorHex());
    }

    // ── Import: null/empty nested lists ──

    @Test
    void importData_topicWithNullConcepts_skipsConceptCreation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Empty Topic", "Desc", "#000000", null)
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(0, response.conceptsCreated());
        assertEquals(0, response.cardsCreated());
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void importData_topicWithEmptyConcepts_skipsConceptCreation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Empty Topic", "Desc", "#000000", List.of())
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(0, response.conceptsCreated());
        assertEquals(0, response.cardsCreated());
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void importData_conceptWithNullCards_skipsCardCreation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", null, null, List.of(
                        new ImportConceptData("Concept", "Notes", null, null)
                ))
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(1, response.conceptsCreated());
        assertEquals(0, response.cardsCreated());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void importData_conceptWithEmptyCards_skipsCardCreation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", null, null, List.of(
                        new ImportConceptData("Concept", "Notes", null, List.of())
                ))
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(1, response.conceptsCreated());
        assertEquals(0, response.cardsCreated());
        verify(cardRepository, never()).save(any());
    }

    // ── Import: reference explanation ──

    @Test
    void importData_setsReferenceExplanation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", "Notes", "Detailed explanation", null)
                ))
        ));

        ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(conceptCaptor.capture())).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        assertEquals("Detailed explanation", conceptCaptor.getValue().getReferenceExplanation());
    }

    @Test
    void importData_nullReferenceExplanation_doesNotSet() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", null, null, null)
                ))
        ));

        ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(conceptCaptor.capture())).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        assertNull(conceptCaptor.getValue().getReferenceExplanation());
    }

    // ── Import: scale ──

    @Test
    void importData_multipleTopics_countsCorrectly() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic 1", null, null, List.of(
                        new ImportConceptData("C1", null, null, List.of(
                                new ImportCardData("Q1", "A1", null, null, null)
                        )),
                        new ImportConceptData("C2", null, null, List.of(
                                new ImportCardData("Q2", "A2", null, null, null),
                                new ImportCardData("Q3", "A3", null, null, null)
                        ))
                )),
                new ImportTopicData("Topic 2", null, null, List.of(
                        new ImportConceptData("C3", null, null, List.of(
                                new ImportCardData("Q4", "A4", "CODE_OUTPUT", null, null)
                        ))
                ))
        ));

        stubAllSaves();

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(2, response.topicsCreated());
        assertEquals(3, response.conceptsCreated());
        assertEquals(4, response.cardsCreated());
    }

    @Test
    void importData_topicDescription_preserved() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", "A detailed description", "#FF0000", null)
        ));

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        when(topicRepository.save(topicCaptor.capture())).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

        dataPortService.importData(request, testUser);

        assertEquals("A detailed description", topicCaptor.getValue().getDescription());
    }

    @Test
    void importData_conceptNotes_preserved() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("T", null, null, List.of(
                        new ImportConceptData("C", "Important notes", null, null)
                ))
        ));

        ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(conceptCaptor.capture())).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        dataPortService.importData(request, testUser);

        assertEquals("Important notes", conceptCaptor.getValue().getNotes());
    }

    // ── Helpers ──

    private void stubAllSaves() {
        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        lenient().when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });
    }

    private Card makeCard(Concept concept, String front, String back, CardType type) {
        Card card = new Card(concept, testUser, front, back, type, null, null);
        setId(card, UUID.randomUUID());
        return card;
    }

    /** Reflection helper to set UUID id on entities with private id field. */
    private void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}
