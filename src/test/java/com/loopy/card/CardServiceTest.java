package com.loopy.card;

import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.UpdateCardRequest;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.review.service.SchedulingAlgorithm;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.card.service.CardService;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.entity.Topic;
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
 * Unit tests for CardService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ConceptRepository conceptRepository;

    private CardService cardService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private Concept testConcept;
    private UUID conceptId;

    @BeforeEach
    void setUp() {
        cardService = new CardService(cardRepository, conceptRepository);

        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);

        testTopic = new Topic(testUser, "Java", "Java topic", "#6366F1");
        setId(testTopic, UUID.randomUUID());

        testConcept = new Concept(testTopic, testUser, "OOP", "Object Oriented Programming");
        conceptId = UUID.randomUUID();
        setId(testConcept, conceptId);
    }

    @Test
    void createCard_savesWithCorrectConceptAssociation() {
        CreateCardRequest request = new CreateCardRequest(
                conceptId, "What is OOP?", "Object-oriented programming",
                CardType.STANDARD, "Think about objects", null);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            setId(card, UUID.randomUUID());
            return card;
        });

        CardResponse response = cardService.createCard(request, testUser);

        assertNotNull(response);
        assertEquals("What is OOP?", response.front());
        assertEquals("Object-oriented programming", response.back());
        assertEquals("STANDARD", response.cardType());
        assertEquals("Think about objects", response.hint());
        assertEquals(conceptId, response.conceptId());

        verify(conceptRepository).findByIdAndUserId(conceptId, userId);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_conceptNotFound_throwsException() {
        UUID unknownConceptId = UUID.randomUUID();
        CreateCardRequest request = new CreateCardRequest(
                unknownConceptId, "Front", "Back", null, null, null);

        when(conceptRepository.findByIdAndUserId(unknownConceptId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.createCard(request, testUser));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void createCard_defaultCardType_whenNotProvided() {
        CreateCardRequest request = new CreateCardRequest(
                conceptId, "Front", "Back", null, null, null);

        when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            setId(card, UUID.randomUUID());
            return card;
        });

        CardResponse response = cardService.createCard(request, testUser);

        assertEquals("STANDARD", response.cardType());
    }

    @Test
    void getCards_byConcept_returnsList() {
        Card card1 = new Card(testConcept, testUser, "Q1", "A1", CardType.STANDARD, null, null);
        Card card2 = new Card(testConcept, testUser, "Q2", "A2", CardType.CODE_OUTPUT, null, null);
        setId(card1, UUID.randomUUID());
        setId(card2, UUID.randomUUID());

        when(cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, userId))
                .thenReturn(List.of(card1, card2));

        List<CardResponse> cards = cardService.getCards(conceptId, testUser);

        assertEquals(2, cards.size());
        assertEquals("Q1", cards.get(0).front());
        assertEquals("Q2", cards.get(1).front());
    }

    @Test
    void getCard_found_returnsResponse() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Found Q", "Found A", CardType.STANDARD, "Hint", null);
        setId(card, cardId);

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));

        CardResponse response = cardService.getCard(cardId, testUser);

        assertEquals("Found Q", response.front());
        assertEquals("Found A", response.back());
        assertEquals("Hint", response.hint());
    }

    @Test
    void getCard_notFound_throwsException() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.getCard(cardId, testUser));
    }

    @Test
    void updateCard_updatesFields() {
        UUID cardId = UUID.randomUUID();
        Card existing = new Card(testConcept, testUser, "Old Q", "Old A", CardType.STANDARD, null, null);
        setId(existing, cardId);

        UpdateCardRequest request = new UpdateCardRequest(
                "New Q", "New A", CardType.FILL_BLANK, "New Hint", "https://example.com");

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existing));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.updateCard(cardId, request, testUser);

        assertEquals("New Q", response.front());
        assertEquals("New A", response.back());
        assertEquals("FILL_BLANK", response.cardType());
        assertEquals("New Hint", response.hint());
        assertEquals("https://example.com", response.sourceUrl());

        verify(cardRepository).save(existing);
    }

    @Test
    void updateCard_nullCardType_keepsPreviousType() {
        UUID cardId = UUID.randomUUID();
        Card existing = new Card(testConcept, testUser, "Q", "A", CardType.SPOT_THE_BUG, null, null);
        setId(existing, cardId);

        UpdateCardRequest request = new UpdateCardRequest("Updated Q", "Updated A", null, null, null);

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(existing));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.updateCard(cardId, request, testUser);

        assertEquals("SPOT_THE_BUG", response.cardType());
    }

    @Test
    void deleteCard_ownershipValidation_succeeds() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Q", "A", CardType.STANDARD, null, null);
        setId(card, cardId);

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));

        cardService.deleteCard(cardId, testUser);

        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_wrongUser_throwsException() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.deleteCard(cardId, testUser));

        verify(cardRepository, never()).delete(any());
    }

    @Test
    void createCard_differentCardTypes() {
        for (CardType type : CardType.values()) {
            CreateCardRequest request = new CreateCardRequest(
                    conceptId, "Q-" + type.name(), "A-" + type.name(), type, null, null);

            when(conceptRepository.findByIdAndUserId(conceptId, userId)).thenReturn(Optional.of(testConcept));
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
                Card card = invocation.getArgument(0);
                setId(card, UUID.randomUUID());
                return card;
            });

            CardResponse response = cardService.createCard(request, testUser);

            assertEquals(type.name(), response.cardType());
        }
    }

    // --- switchAlgorithm ---

    @Test
    void switchAlgorithm_toFSRS_updatesAndReturns() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Q", "A", CardType.STANDARD, null, null);
        setId(card, cardId);

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.switchAlgorithm(cardId, SchedulingAlgorithm.FSRS, testUser);

        assertNotNull(response);
        assertEquals("FSRS", response.schedulingAlgorithm());
        assertEquals(cardId, response.id());
        verify(cardRepository).save(card);
    }

    @Test
    void switchAlgorithm_toSM2_updatesAndReturns() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Q", "A", CardType.STANDARD, null, null);
        setId(card, cardId);
        card.setSchedulingAlgorithm(SchedulingAlgorithm.FSRS);

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.switchAlgorithm(cardId, SchedulingAlgorithm.SM2, testUser);

        assertEquals("SM2", response.schedulingAlgorithm());
        verify(cardRepository).save(card);
    }

    @Test
    void switchAlgorithm_cardNotFound_throwsException() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cardService.switchAlgorithm(cardId, SchedulingAlgorithm.FSRS, testUser));

        verify(cardRepository, never()).save(any());
    }

    @Test
    void switchAlgorithm_sameAlgorithm_stillSaves() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Q", "A", CardType.STANDARD, null, null);
        setId(card, cardId);
        // Default is SM2

        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.switchAlgorithm(cardId, SchedulingAlgorithm.SM2, testUser);

        assertEquals("SM2", response.schedulingAlgorithm());
        verify(cardRepository).save(card);
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
