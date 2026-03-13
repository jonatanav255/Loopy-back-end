package com.loopy.card;

import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.card.service.ConceptService;
import com.loopy.config.ReorderRequest;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConceptService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class ConceptServiceTest {

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private TopicRepository topicRepository;

    private ConceptService conceptService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private UUID topicId;

    @BeforeEach
    void setUp() {
        conceptService = new ConceptService(conceptRepository, topicRepository);

        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);

        testTopic = new Topic(testUser, "Java", "Java topic", "#6366F1");
        topicId = UUID.randomUUID();
        setId(testTopic, topicId);
    }

    @Test
    void createConcept_savesWithCorrectTopicAssociation() {
        CreateConceptRequest request = new CreateConceptRequest(topicId, "Polymorphism", "OOP concept");

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(testTopic));
        when(conceptRepository.save(any(Concept.class))).thenAnswer(invocation -> {
            Concept c = invocation.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        ConceptResponse response = conceptService.createConcept(request, testUser);

        assertNotNull(response);
        assertEquals("Polymorphism", response.title());
        assertEquals("OOP concept", response.notes());
        assertEquals(topicId, response.topicId());
        assertEquals("LEARNING", response.status());

        verify(topicRepository).findByIdAndUserId(topicId, userId);
        verify(conceptRepository).save(any(Concept.class));
    }

    @Test
    void createConcept_topicNotFound_throwsException() {
        UUID unknownTopicId = UUID.randomUUID();
        CreateConceptRequest request = new CreateConceptRequest(unknownTopicId, "Title", "Notes");

        when(topicRepository.findByIdAndUserId(unknownTopicId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conceptService.createConcept(request, testUser));

        verify(conceptRepository, never()).save(any());
    }

    @Test
    void getConcepts_byTopic_returnsList() {
        Concept c1 = new Concept(testTopic, testUser, "Concept A", "Notes A");
        Concept c2 = new Concept(testTopic, testUser, "Concept B", "Notes B");
        setId(c1, UUID.randomUUID());
        setId(c2, UUID.randomUUID());

        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topicId, userId))
                .thenReturn(List.of(c1, c2));

        List<ConceptResponse> concepts = conceptService.getConcepts(topicId, testUser);

        assertEquals(2, concepts.size());
        assertEquals("Concept A", concepts.get(0).title());
        assertEquals("Concept B", concepts.get(1).title());
    }

    @Test
    void getConcept_found_returnsResponse() {
        UUID conceptId = UUID.randomUUID();
        Concept concept = new Concept(testTopic, testUser, "Found It", "Notes");
        setId(concept, conceptId);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(concept));

        ConceptResponse response = conceptService.getConcept(conceptId, testUser);

        assertEquals("Found It", response.title());
        assertEquals(conceptId, response.id());
    }

    @Test
    void getConcept_notFound_throwsException() {
        UUID conceptId = UUID.randomUUID();
        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conceptService.getConcept(conceptId, testUser));
    }

    @Test
    void updateConcept_updatesFields() {
        UUID conceptId = UUID.randomUUID();
        Concept existing = new Concept(testTopic, testUser, "Old Title", "Old Notes");
        setId(existing, conceptId);

        UpdateConceptRequest request = new UpdateConceptRequest("New Title", "New Notes", "Reference text");

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(existing));
        when(conceptRepository.save(any(Concept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConceptResponse response = conceptService.updateConcept(conceptId, request, testUser);

        assertEquals("New Title", response.title());
        assertEquals("New Notes", response.notes());
        assertEquals("Reference text", response.referenceExplanation());

        verify(conceptRepository).save(existing);
    }

    @Test
    void deleteConcept_ownershipValidation_succeeds() {
        UUID conceptId = UUID.randomUUID();
        Concept concept = new Concept(testTopic, testUser, "To Delete", "Notes");
        setId(concept, conceptId);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(concept));

        conceptService.deleteConcept(conceptId, testUser);

        verify(conceptRepository).delete(concept);
    }

    @Test
    void deleteConcept_wrongUser_throwsException() {
        UUID conceptId = UUID.randomUUID();
        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conceptService.deleteConcept(conceptId, testUser));

        verify(conceptRepository, never()).delete(any());
    }

    @Test
    void reorderConcepts_updatesSortOrderCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Concept c1 = new Concept(testTopic, testUser, "Concept 1", "Notes 1");
        Concept c2 = new Concept(testTopic, testUser, "Concept 2", "Notes 2");
        Concept c3 = new Concept(testTopic, testUser, "Concept 3", "Notes 3");
        setId(c1, id1);
        setId(c2, id2);
        setId(c3, id3);

        // Request reorder: c3 first, c1 second, c2 third
        List<UUID> orderedIds = List.of(id3, id1, id2);
        ReorderRequest request = new ReorderRequest(orderedIds);

        when(topicRepository.findByIdAndUserId(topicId, userId))
                .thenReturn(Optional.of(testTopic));
        when(conceptRepository.findAllByUserIdAndIdIn(userId, orderedIds))
                .thenReturn(List.of(c1, c2, c3));
        when(conceptRepository.saveAll(any())).thenReturn(List.of(c3, c1, c2));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topicId, userId))
                .thenReturn(List.of(c3, c1, c2));

        List<ConceptResponse> result = conceptService.reorderConcepts(topicId, request, testUser);

        // Verify sort orders were set based on position in orderedIds
        assertEquals(2, c1.getSortOrder()); // id1 is at index 1 → sortOrder 2
        assertEquals(3, c2.getSortOrder()); // id2 is at index 2 → sortOrder 3
        assertEquals(1, c3.getSortOrder()); // id3 is at index 0 → sortOrder 1

        verify(conceptRepository).saveAll(any());
        assertEquals(3, result.size());
    }

    @Test
    void reorderConcepts_topicNotFound_throwsException() {
        List<UUID> orderedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        ReorderRequest request = new ReorderRequest(orderedIds);

        when(topicRepository.findByIdAndUserId(topicId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conceptService.reorderConcepts(topicId, request, testUser));

        verify(conceptRepository, never()).saveAll(any());
    }

    @Test
    void reorderConcepts_missingConcept_throwsException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Concept c1 = new Concept(testTopic, testUser, "Concept 1", "Notes 1");
        Concept c2 = new Concept(testTopic, testUser, "Concept 2", "Notes 2");
        setId(c1, id1);
        setId(c2, id2);

        // Request has 3 IDs but only 2 concepts exist
        List<UUID> orderedIds = List.of(id1, id2, id3);
        ReorderRequest request = new ReorderRequest(orderedIds);

        when(topicRepository.findByIdAndUserId(topicId, userId))
                .thenReturn(Optional.of(testTopic));
        when(conceptRepository.findAllByUserIdAndIdIn(userId, orderedIds))
                .thenReturn(List.of(c1, c2));

        assertThrows(ResourceNotFoundException.class,
                () -> conceptService.reorderConcepts(topicId, request, testUser));

        verify(conceptRepository, never()).saveAll(any());
    }

    @Test
    void createConcept_assignsNextSortOrder() {
        Concept existing1 = new Concept(testTopic, testUser, "Existing 1", "Notes");
        Concept existing2 = new Concept(testTopic, testUser, "Existing 2", "Notes");
        setId(existing1, UUID.randomUUID());
        setId(existing2, UUID.randomUUID());
        existing1.setSortOrder(1);
        existing2.setSortOrder(2);

        when(topicRepository.findByIdAndUserId(topicId, userId))
                .thenReturn(Optional.of(testTopic));
        when(conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topicId, userId))
                .thenReturn(List.of(existing1, existing2));

        ArgumentCaptor<Concept> conceptCaptor = ArgumentCaptor.forClass(Concept.class);
        when(conceptRepository.save(conceptCaptor.capture())).thenAnswer(invocation -> {
            Concept c = invocation.getArgument(0);
            setId(c, UUID.randomUUID());
            return c;
        });

        CreateConceptRequest request = new CreateConceptRequest(topicId, "New Concept", "New Notes");
        conceptService.createConcept(request, testUser);

        Concept savedConcept = conceptCaptor.getValue();
        assertEquals(3, savedConcept.getSortOrder());
        assertEquals("New Concept", savedConcept.getTitle());
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
