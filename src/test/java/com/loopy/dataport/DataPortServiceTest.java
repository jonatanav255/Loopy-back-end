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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    // ── Export tests ──

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
        assertEquals(1, topicData.concepts().size());

        ExportConceptData conceptData = topicData.concepts().get(0);
        assertEquals("OOP", conceptData.title());
        assertEquals(1, conceptData.cards().size());

        ExportCardData cardData = conceptData.cards().get(0);
        assertEquals("What is OOP?", cardData.front());
        assertEquals("A paradigm", cardData.back());
        assertEquals("STANDARD", cardData.cardType());
    }

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
    void exportData_topicNotFound_throwsException() {
        UUID badId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(badId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dataPortService.exportData(testUser, List.of(badId)));
    }

    @Test
    void exportData_emptyAccount_returnsEmptyExport() {
        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of());

        ExportResponse response = dataPortService.exportData(testUser, null);

        assertEquals(0, response.topicCount());
        assertEquals(0, response.conceptCount());
        assertEquals(0, response.cardCount());
        assertTrue(response.topics().isEmpty());
    }

    // ── Import tests ──

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
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(1, response.conceptsCreated());
        assertEquals(2, response.cardsCreated());

        verify(topicRepository).save(any(Topic.class));
        verify(conceptRepository).save(any(Concept.class));
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void importData_defaultColor_whenNull() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("No Color", "Desc", null, null)
        ));

        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            assertEquals("#6366F1", t.getColorHex());
            return t;
        });

        dataPortService.importData(request, testUser);

        verify(topicRepository).save(any(Topic.class));
    }

    @Test
    void importData_unknownCardType_fallsBackToStandard() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", null, null, List.of(
                        new ImportConceptData("Concept", null, null, List.of(
                                new ImportCardData("Q", "A", "NONEXISTENT_TYPE", null, null)
                        ))
                ))
        ));

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
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card card = inv.getArgument(0);
            setId(card, UUID.randomUUID());
            assertEquals(CardType.STANDARD, card.getCardType());
            return card;
        });

        dataPortService.importData(request, testUser);

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void importData_topicWithNullConcepts_skipsConceptCreation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Empty Topic", "Desc", "#000000", null)
        ));

        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

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

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(1, response.topicsCreated());
        assertEquals(1, response.conceptsCreated());
        assertEquals(0, response.cardsCreated());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void importData_setsReferenceExplanation() {
        ImportRequest request = new ImportRequest("1.0", List.of(
                new ImportTopicData("Topic", null, null, List.of(
                        new ImportConceptData("Concept", "Notes", "Detailed explanation", null)
                ))
        ));

        when(topicRepository.save(any(Topic.class))).thenAnswer(inv -> {
            Topic t = inv.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(inv -> {
            Concept c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            assertEquals("Detailed explanation", c.getReferenceExplanation());
            return c;
        });

        dataPortService.importData(request, testUser);
    }

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
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        ImportResponse response = dataPortService.importData(request, testUser);

        assertEquals(2, response.topicsCreated());
        assertEquals(3, response.conceptsCreated());
        assertEquals(4, response.cardsCreated());
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
