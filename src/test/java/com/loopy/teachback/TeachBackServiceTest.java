package com.loopy.teachback;

import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.teachback.dto.TeachBackResponse;
import com.loopy.teachback.entity.TeachBack;
import com.loopy.teachback.repository.TeachBackRepository;
import com.loopy.teachback.service.TeachBackService;
import com.loopy.topic.entity.Topic;
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
 * Unit tests for TeachBackService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class TeachBackServiceTest {

    @Mock
    private TeachBackRepository teachBackRepository;

    @Mock
    private ConceptRepository conceptRepository;

    private TeachBackService teachBackService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private Concept testConcept;
    private UUID conceptId;

    @BeforeEach
    void setUp() {
        teachBackService = new TeachBackService(teachBackRepository, conceptRepository);

        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);

        testTopic = new Topic(testUser, "Java", "Java topic", "#6366F1");
        setId(testTopic, UUID.randomUUID());

        testConcept = new Concept(testTopic, testUser, "OOP", "Object Oriented Programming");
        conceptId = UUID.randomUUID();
        setId(testConcept, conceptId);
        testConcept.setStatus(ConceptStatus.TEACH_BACK_REQUIRED);
    }

    // --- getConceptsRequiringTeachBack ---

    @Test
    void getConceptsRequiringTeachBack_returnsConcepts() {
        when(conceptRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, ConceptStatus.TEACH_BACK_REQUIRED))
                .thenReturn(List.of(testConcept));

        List<ConceptResponse> result = teachBackService.getConceptsRequiringTeachBack(testUser);

        assertEquals(1, result.size());
        assertEquals("OOP", result.get(0).title());
        assertEquals(conceptId, result.get(0).id());
        assertEquals("TEACH_BACK_REQUIRED", result.get(0).status());
    }

    @Test
    void getConceptsRequiringTeachBack_emptyList_returnsEmpty() {
        when(conceptRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, ConceptStatus.TEACH_BACK_REQUIRED))
                .thenReturn(List.of());

        List<ConceptResponse> result = teachBackService.getConceptsRequiringTeachBack(testUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getConceptsRequiringTeachBack_multipleConcepts() {
        Concept concept2 = new Concept(testTopic, testUser, "Polymorphism", "Notes");
        setId(concept2, UUID.randomUUID());
        concept2.setStatus(ConceptStatus.TEACH_BACK_REQUIRED);

        when(conceptRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, ConceptStatus.TEACH_BACK_REQUIRED))
                .thenReturn(List.of(testConcept, concept2));

        List<ConceptResponse> result = teachBackService.getConceptsRequiringTeachBack(testUser);

        assertEquals(2, result.size());
        assertEquals("OOP", result.get(0).title());
        assertEquals("Polymorphism", result.get(1).title());
    }

    // --- submitTeachBack ---

    @Test
    void submitTeachBack_highRating_movesConceptToReview() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "OOP is about objects", 4, List.of("gap1"));

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeachBackResponse response = teachBackService.submitTeachBack(request, testUser);

        assertNotNull(response);
        assertEquals("OOP is about objects", response.userExplanation());
        assertEquals(4, response.selfRating());
        assertEquals(List.of("gap1"), response.gapsFound());

        // Concept should be moved to REVIEW
        assertEquals(ConceptStatus.REVIEW, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    @Test
    void submitTeachBack_rating5_alsoMovesConceptToReview() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "I understand OOP perfectly", 5, null);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });
        when(conceptRepository.save(any(Concept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teachBackService.submitTeachBack(request, testUser);

        assertEquals(ConceptStatus.REVIEW, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    @Test
    void submitTeachBack_lowRating_doesNotMoveConceptToReview() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "I'm not sure about OOP", 3, List.of("gap1", "gap2"));

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });

        teachBackService.submitTeachBack(request, testUser);

        // Concept should stay at TEACH_BACK_REQUIRED
        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository, never()).save(testConcept);
    }

    @Test
    void submitTeachBack_rating1_keepsTeachBackRequired() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "I have no idea", 1, null);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });

        teachBackService.submitTeachBack(request, testUser);

        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository, never()).save(testConcept);
    }

    @Test
    void submitTeachBack_conceptNotFound_throwsException() {
        UUID unknownConceptId = UUID.randomUUID();
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                unknownConceptId, "Explanation", 4, null);

        when(conceptRepository.findByIdAndUserId(unknownConceptId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teachBackService.submitTeachBack(request, testUser));

        verify(teachBackRepository, never()).save(any());
    }

    @Test
    void submitTeachBack_nullGapsFound_savesNullGaps() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "Explanation", 3, null);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });

        teachBackService.submitTeachBack(request, testUser);

        ArgumentCaptor<TeachBack> captor = ArgumentCaptor.forClass(TeachBack.class);
        verify(teachBackRepository).save(captor.capture());
        assertNull(captor.getValue().getGapsFound());
    }

    @Test
    void submitTeachBack_emptyGapsFound_savesNullGaps() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "Explanation", 3, List.of());

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });

        teachBackService.submitTeachBack(request, testUser);

        ArgumentCaptor<TeachBack> captor = ArgumentCaptor.forClass(TeachBack.class);
        verify(teachBackRepository).save(captor.capture());
        assertNull(captor.getValue().getGapsFound());
    }

    @Test
    void submitTeachBack_multipleGaps_joinedWithPipe() {
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "Explanation", 3, List.of("gap1", "gap2", "gap3"));

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.save(any(TeachBack.class))).thenAnswer(invocation -> {
            TeachBack tb = invocation.getArgument(0);
            setId(tb, UUID.randomUUID());
            return tb;
        });

        teachBackService.submitTeachBack(request, testUser);

        ArgumentCaptor<TeachBack> captor = ArgumentCaptor.forClass(TeachBack.class);
        verify(teachBackRepository).save(captor.capture());
        assertEquals("gap1|gap2|gap3", captor.getValue().getGapsFound());
    }

    // --- getHistory ---

    @Test
    void getHistory_returnsTeachBackHistory() {
        TeachBack tb1 = new TeachBack(testConcept, testUser, "Explanation 1", 3, "gap1|gap2");
        TeachBack tb2 = new TeachBack(testConcept, testUser, "Explanation 2", 5, null);
        setId(tb1, UUID.randomUUID());
        setId(tb2, UUID.randomUUID());

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, userId))
                .thenReturn(List.of(tb1, tb2));

        List<TeachBackResponse> result = teachBackService.getHistory(conceptId, testUser);

        assertEquals(2, result.size());
        assertEquals("Explanation 1", result.get(0).userExplanation());
        assertEquals(3, result.get(0).selfRating());
        assertEquals(List.of("gap1", "gap2"), result.get(0).gapsFound());
        assertEquals("Explanation 2", result.get(1).userExplanation());
        assertEquals(5, result.get(1).selfRating());
        assertTrue(result.get(1).gapsFound().isEmpty());
    }

    @Test
    void getHistory_conceptNotFound_throwsException() {
        UUID unknownConceptId = UUID.randomUUID();

        when(conceptRepository.findByIdAndUserId(unknownConceptId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teachBackService.getHistory(unknownConceptId, testUser));

        verify(teachBackRepository, never()).findByConceptIdAndUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void getHistory_emptyHistory_returnsEmptyList() {
        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(teachBackRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, userId))
                .thenReturn(List.of());

        List<TeachBackResponse> result = teachBackService.getHistory(conceptId, testUser);

        assertTrue(result.isEmpty());
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
