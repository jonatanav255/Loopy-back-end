package com.loopy.review;

import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.CardRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.review.dto.ReviewResponse;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import com.loopy.review.service.*;
import com.loopy.teachback.service.EscalationService;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private SM2Service sm2Service;

    @Mock
    private FSRSService fsrsService;

    @Mock
    private EscalationService escalationService;

    private ReviewService reviewService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private Concept testConcept;
    private Card testCard;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                cardRepository, reviewLogRepository, sm2Service, fsrsService, escalationService);

        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);

        testTopic = new Topic(testUser, "Java", "Java topic", "#6366F1");
        setId(testTopic, UUID.randomUUID());

        testConcept = new Concept(testTopic, testUser, "OOP", "Notes");
        setId(testConcept, UUID.randomUUID());

        testCard = new Card(testConcept, testUser, "Q", "A", CardType.STANDARD, null, null);
        cardId = UUID.randomUUID();
        setId(testCard, cardId);
    }

    @Test
    void getDueCards_returnsOnlyDueCards() {
        Card dueCard = new Card(testConcept, testUser, "Due Q", "Due A", CardType.STANDARD, null, null);
        setId(dueCard, UUID.randomUUID());

        when(cardRepository.findDueCards(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(dueCard));

        List<CardResponse> result = reviewService.getDueCards(testUser, null, null);

        assertEquals(1, result.size());
        assertEquals("Due Q", result.get(0).front());
    }

    @Test
    void getDueCards_withTopicFilter() {
        UUID topicId = UUID.randomUUID();
        Card card = new Card(testConcept, testUser, "Topic Q", "Topic A", CardType.STANDARD, null, null);
        setId(card, UUID.randomUUID());

        when(cardRepository.findDueCardsByTopics(eq(userId), any(LocalDate.class), eq(List.of(topicId))))
                .thenReturn(List.of(card));

        List<CardResponse> result = reviewService.getDueCards(testUser, List.of(topicId), null);

        assertEquals(1, result.size());
        assertEquals("Topic Q", result.get(0).front());
        verify(cardRepository).findDueCardsByTopics(eq(userId), any(LocalDate.class), eq(List.of(topicId)));
    }

    @Test
    void getDueCards_withLimit() {
        Card card1 = new Card(testConcept, testUser, "Q1", "A1", CardType.STANDARD, null, null);
        Card card2 = new Card(testConcept, testUser, "Q2", "A2", CardType.STANDARD, null, null);
        Card card3 = new Card(testConcept, testUser, "Q3", "A3", CardType.STANDARD, null, null);
        setId(card1, UUID.randomUUID());
        setId(card2, UUID.randomUUID());
        setId(card3, UUID.randomUUID());

        when(cardRepository.findDueCards(eq(userId), any(LocalDate.class)))
                .thenReturn(List.of(card1, card2, card3));

        List<CardResponse> result = reviewService.getDueCards(testUser, null, 2);

        assertEquals(2, result.size());
    }

    @Test
    void submitReview_withSM2_createsLogAndUpdatesCard() {
        SubmitReviewRequest request = new SubmitReviewRequest(4, 2000L, 2);
        LocalDate today = LocalDate.now();

        SM2Result sm2Result = new SM2Result(1, 2.5, 1, today.plusDays(1));
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(testCard));
        when(sm2Service.calculate(eq(0), eq(2.5), eq(0), eq(4), any(LocalDate.class)))
                .thenReturn(sm2Result);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(ReviewLog.class))).thenAnswer(invocation -> {
            ReviewLog log = invocation.getArgument(0);
            setId(log, UUID.randomUUID());
            return log;
        });

        ReviewResponse response = reviewService.submitReview(cardId, request, testUser);

        assertNotNull(response);
        assertEquals(4, response.rating());
        assertEquals(2000L, response.responseTimeMs());
        assertEquals(2, response.confidence());

        // Verify card was updated
        verify(cardRepository).save(testCard);
        assertEquals(1, testCard.getRepetitionCount());
        assertEquals(2.5, testCard.getEaseFactor());
        assertEquals(1, testCard.getIntervalDays());
        assertEquals(today, testCard.getLastReviewDate());

        // Verify review log was saved
        verify(reviewLogRepository).save(any(ReviewLog.class));

        // Verify escalation check was called
        verify(escalationService).checkAndEscalate(testCard);
    }

    @Test
    void submitReview_withFSRS_usesCorrectAlgorithm() {
        testCard.setSchedulingAlgorithm(SchedulingAlgorithm.FSRS);

        SubmitReviewRequest request = new SubmitReviewRequest(4, 1500L, 3);
        LocalDate today = LocalDate.now();

        SchedulingResult fsrsResult = new SchedulingResult(1, 2.5, 3, today.plusDays(3), 3.12, 5.5);
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(testCard));
        when(fsrsService.calculate(eq(0.0), eq(0.0), eq(0), eq(4), eq(0), any(LocalDate.class)))
                .thenReturn(fsrsResult);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(ReviewLog.class))).thenAnswer(invocation -> {
            ReviewLog log = invocation.getArgument(0);
            setId(log, UUID.randomUUID());
            return log;
        });

        ReviewResponse response = reviewService.submitReview(cardId, request, testUser);

        assertNotNull(response);

        // Verify FSRS was called, not SM2
        verify(fsrsService).calculate(anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt(), any(LocalDate.class));
        verify(sm2Service, never()).calculate(anyInt(), anyDouble(), anyInt(), anyInt(), any(LocalDate.class));

        // Verify card got FSRS-specific fields
        assertEquals(3.12, testCard.getStability());
        assertEquals(5.5, testCard.getDifficulty());
    }

    @Test
    void submitReview_cardNotFound_throwsException() {
        SubmitReviewRequest request = new SubmitReviewRequest(4, 1000L, 2);
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.submitReview(cardId, request, testUser));

        verify(reviewLogRepository, never()).save(any());
    }

    @Test
    void submitReview_reviewLogContainsCorrectData() {
        SubmitReviewRequest request = new SubmitReviewRequest(3, 3500L, 1);

        SM2Result sm2Result = new SM2Result(1, 2.36, 1, LocalDate.now().plusDays(1));
        when(cardRepository.findByIdAndUserId(cardId, userId)).thenReturn(Optional.of(testCard));
        when(sm2Service.calculate(anyInt(), anyDouble(), anyInt(), anyInt(), any(LocalDate.class)))
                .thenReturn(sm2Result);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewLogRepository.save(any(ReviewLog.class))).thenAnswer(invocation -> {
            ReviewLog log = invocation.getArgument(0);
            setId(log, UUID.randomUUID());
            return log;
        });

        reviewService.submitReview(cardId, request, testUser);

        ArgumentCaptor<ReviewLog> logCaptor = ArgumentCaptor.forClass(ReviewLog.class);
        verify(reviewLogRepository).save(logCaptor.capture());

        ReviewLog savedLog = logCaptor.getValue();
        assertEquals(3, savedLog.getRating());
        assertEquals(3500L, savedLog.getResponseTimeMs());
        assertEquals(1, savedLog.getConfidence());
        assertEquals(testCard, savedLog.getCard());
        assertEquals(testUser, savedLog.getUser());
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
