package com.loopy.teachback;

import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import com.loopy.teachback.service.EscalationService;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EscalationService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private ConceptRepository conceptRepository;

    private EscalationService escalationService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private Concept testConcept;
    private Card testCard;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        escalationService = new EscalationService(reviewLogRepository, conceptRepository);

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

    // --- Failure threshold (rating < 3) ---

    @Test
    void checkAndEscalate_threeFailures_escalatesToTeachBack() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(1, null),
                createReviewLog(2, null),
                createReviewLog(1, null)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    @Test
    void checkAndEscalate_twoFailures_doesNotEscalate() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(1, null),
                createReviewLog(2, null),
                createReviewLog(4, null)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void checkAndEscalate_moreThanThreeFailures_stillEscalates() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(1, null),
                createReviewLog(2, null),
                createReviewLog(1, null),
                createReviewLog(2, null)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    // --- Low confidence threshold (confidence = 1) ---

    @Test
    void checkAndEscalate_threeLowConfidence_escalatesToTeachBack() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(4, 1),
                createReviewLog(3, 1),
                createReviewLog(5, 1)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    @Test
    void checkAndEscalate_twoLowConfidence_doesNotEscalate() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(4, 1),
                createReviewLog(3, 1),
                createReviewLog(5, 3)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    @Test
    void checkAndEscalate_nullConfidence_notCountedAsLow() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(4, null),
                createReviewLog(3, null),
                createReviewLog(5, 1)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    // --- Mixed failures and low confidence ---

    @Test
    void checkAndEscalate_mixedFailuresAndLowConfidence_belowBothThresholds() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(1, 1),  // counts as both failure and low confidence
                createReviewLog(2, 1),  // same
                createReviewLog(4, 3)   // neither
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        // 2 failures, 2 low confidence — both below threshold of 3
        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    // --- Already at TEACH_BACK_REQUIRED ---

    @Test
    void checkAndEscalate_alreadyTeachBackRequired_doesNotReEscalate() {
        testConcept.setStatus(ConceptStatus.TEACH_BACK_REQUIRED);

        escalationService.checkAndEscalate(testCard);

        // Should not query reviews at all
        verify(reviewLogRepository, never()).findByCardIdAndUserIdOrderByReviewedAtDesc(any(), any());
        verify(conceptRepository, never()).save(any());
    }

    // --- No reviews ---

    @Test
    void checkAndEscalate_noReviews_doesNotEscalate() {
        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(List.of());

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    // --- Concept in REVIEW status ---

    @Test
    void checkAndEscalate_conceptInReviewStatus_canEscalate() {
        testConcept.setStatus(ConceptStatus.REVIEW);

        List<ReviewLog> reviews = List.of(
                createReviewLog(1, null),
                createReviewLog(2, null),
                createReviewLog(1, null)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.TEACH_BACK_REQUIRED, testConcept.getStatus());
        verify(conceptRepository).save(testConcept);
    }

    // --- All passing reviews ---

    @Test
    void checkAndEscalate_allPassingReviews_doesNotEscalate() {
        List<ReviewLog> reviews = List.of(
                createReviewLog(4, 3),
                createReviewLog(5, 2),
                createReviewLog(3, 3)
        );

        when(reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(cardId, userId))
                .thenReturn(reviews);

        escalationService.checkAndEscalate(testCard);

        assertEquals(ConceptStatus.LEARNING, testConcept.getStatus());
        verify(conceptRepository, never()).save(any());
    }

    /** Creates a ReviewLog with the given rating and confidence for the test card. */
    private ReviewLog createReviewLog(int rating, Integer confidence) {
        ReviewLog log = new ReviewLog(testCard, testUser, rating, 1000L, confidence);
        setId(log, UUID.randomUUID());
        return log;
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
