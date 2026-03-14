package com.loopy.review;

import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.CardRepository;
import com.loopy.review.dto.FragileCard;
import com.loopy.review.dto.HeatmapEntry;
import com.loopy.review.dto.StatsOverview;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import com.loopy.review.service.StatsService;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StatsService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @Mock
    private CardRepository cardRepository;

    private StatsService statsService;

    private User testUser;
    private UUID userId;
    private Topic testTopic;
    private Concept testConcept;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(reviewLogRepository, cardRepository);

        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);

        testTopic = new Topic(testUser, "Java", "desc", "#6366F1");
        setId(testTopic, UUID.randomUUID());

        testConcept = new Concept(testTopic, testUser, "OOP", "Notes");
        setId(testConcept, UUID.randomUUID());
    }

    @Test
    void getOverview_calculatesCorrectStats() {
        when(cardRepository.countDueCards(eq(userId), any(LocalDate.class)))
                .thenReturn(3L);
        when(cardRepository.countByUserId(userId))
                .thenReturn(5L);
        when(reviewLogRepository.countByUserIdAndReviewedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(10L);
        when(reviewLogRepository.countPassedByUserInRange(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(8L);
        when(reviewLogRepository.findAllByUserIdOrdered(userId))
                .thenReturn(Collections.emptyList());

        StatsOverview overview = statsService.getOverview(testUser);

        assertEquals(3, overview.cardsDueToday());
        assertEquals(10, overview.cardsReviewedToday());
        assertEquals(5, overview.totalCards());
        assertEquals(80.0, overview.accuracyToday(), 0.1);
        assertEquals(0, overview.currentStreak());
        assertEquals(0, overview.longestStreak());
    }

    @Test
    void getOverview_emptyData_returnsZeros() {
        when(cardRepository.countDueCards(eq(userId), any(LocalDate.class)))
                .thenReturn(0L);
        when(cardRepository.countByUserId(userId))
                .thenReturn(0L);
        when(reviewLogRepository.countByUserIdAndReviewedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(reviewLogRepository.countPassedByUserInRange(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(reviewLogRepository.findAllByUserIdOrdered(userId))
                .thenReturn(Collections.emptyList());

        StatsOverview overview = statsService.getOverview(testUser);

        assertEquals(0, overview.cardsDueToday());
        assertEquals(0, overview.cardsReviewedToday());
        assertEquals(0, overview.totalCards());
        assertEquals(0.0, overview.accuracyToday());
        assertEquals(0, overview.currentStreak());
        assertEquals(0, overview.longestStreak());
    }

    @Test
    void getOverview_accuracyCalculation_zeroReviews() {
        when(cardRepository.countDueCards(eq(userId), any(LocalDate.class)))
                .thenReturn(0L);
        when(cardRepository.countByUserId(userId))
                .thenReturn(0L);
        when(reviewLogRepository.countByUserIdAndReviewedAtBetween(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(reviewLogRepository.countPassedByUserInRange(eq(userId), any(Instant.class), any(Instant.class)))
                .thenReturn(0L);
        when(reviewLogRepository.findAllByUserIdOrdered(userId))
                .thenReturn(Collections.emptyList());

        StatsOverview overview = statsService.getOverview(testUser);

        // When no reviews today, accuracy should be 0, not NaN
        assertEquals(0.0, overview.accuracyToday());
    }

    @Test
    void getHeatmap_returnsCorrectDateCountMapping() {
        LocalDate today = LocalDate.now();
        Instant todayInstant = today.atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(3600);
        Instant yesterdayInstant = today.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().plusSeconds(3600);

        ReviewLog log1 = makeReviewLog(todayInstant);
        ReviewLog log2 = makeReviewLog(todayInstant);
        ReviewLog log3 = makeReviewLog(yesterdayInstant);

        when(reviewLogRepository.findAllByUserIdOrdered(userId))
                .thenReturn(List.of(log3, log1, log2));

        List<HeatmapEntry> heatmap = statsService.getHeatmap(testUser);

        assertEquals(2, heatmap.size());
        // Sorted by date
        assertEquals(today.minusDays(1), heatmap.get(0).date());
        assertEquals(1, heatmap.get(0).count());
        assertEquals(today, heatmap.get(1).date());
        assertEquals(2, heatmap.get(1).count());
    }

    @Test
    void getHeatmap_emptyReviews_returnsEmptyList() {
        when(reviewLogRepository.findAllByUserIdOrdered(userId))
                .thenReturn(Collections.emptyList());

        List<HeatmapEntry> heatmap = statsService.getHeatmap(testUser);

        assertTrue(heatmap.isEmpty());
    }

    @Test
    void getFragileCards_returnsCardsWithLowConfidence() {
        Card fragileCard = makeCard("Fragile Q");
        UUID fragileCardId = UUID.randomUUID();
        setId(fragileCard, fragileCardId);

        ReviewLog fragileReview = new ReviewLog(fragileCard, testUser, 4, 1500L, 1);
        setId(fragileReview, UUID.randomUUID());

        when(reviewLogRepository.findFragileReviews(userId))
                .thenReturn(List.of(fragileReview));

        List<FragileCard> fragileCards = statsService.getFragileCards(testUser);

        assertEquals(1, fragileCards.size());
        assertEquals(4, fragileCards.get(0).lastRating());
        assertEquals(1, fragileCards.get(0).lastConfidence());
        assertEquals(1, fragileCards.get(0).occurrences());
    }

    @Test
    void getFragileCards_multipleOccurrencesForSameCard() {
        Card fragileCard = makeCard("Fragile Q");
        UUID fragileCardId = UUID.randomUUID();
        setId(fragileCard, fragileCardId);

        ReviewLog review1 = new ReviewLog(fragileCard, testUser, 3, 2000L, 1);
        ReviewLog review2 = new ReviewLog(fragileCard, testUser, 4, 1500L, 1);
        setId(review1, UUID.randomUUID());
        setId(review2, UUID.randomUUID());

        when(reviewLogRepository.findFragileReviews(userId))
                .thenReturn(List.of(review1, review2));

        List<FragileCard> fragileCards = statsService.getFragileCards(testUser);

        assertEquals(1, fragileCards.size());
        assertEquals(2, fragileCards.get(0).occurrences());
    }

    @Test
    void getFragileCards_emptyReviews_returnsEmptyList() {
        when(reviewLogRepository.findFragileReviews(userId))
                .thenReturn(Collections.emptyList());

        List<FragileCard> fragileCards = statsService.getFragileCards(testUser);

        assertTrue(fragileCards.isEmpty());
    }

    /** Creates a Card with a generated UUID. */
    private Card makeCard(String front) {
        Card card = new Card(testConcept, testUser, front, "Answer", CardType.STANDARD, null, null);
        setId(card, UUID.randomUUID());
        return card;
    }

    /** Creates a ReviewLog with a specific reviewedAt time. */
    private ReviewLog makeReviewLog(Instant reviewedAt) {
        Card card = makeCard("Q");
        ReviewLog log = new ReviewLog(card, testUser, 4, 1000L, 2);
        setId(log, UUID.randomUUID());
        // Set reviewedAt via reflection since it defaults to Instant.now()
        try {
            java.lang.reflect.Field field = ReviewLog.class.getDeclaredField("reviewedAt");
            field.setAccessible(true);
            field.set(log, reviewedAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set reviewedAt", e);
        }
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
