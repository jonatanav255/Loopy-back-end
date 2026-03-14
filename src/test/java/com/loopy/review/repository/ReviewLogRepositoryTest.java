package com.loopy.review.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.review.entity.ReviewLog;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for ReviewLogRepository using @DataJpaTest.
 */
@DataJpaTest
class ReviewLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReviewLogRepository reviewLogRepository;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "hashed");
        entityManager.persist(user);

        Topic topic = new Topic(user, "Java", null, "#FF0000");
        entityManager.persist(topic);

        Concept concept = new Concept(topic, user, "OOP", null);
        entityManager.persist(concept);

        card = new Card(concept, user, "Q", "A", CardType.STANDARD, null, null);
        entityManager.persist(card);

        entityManager.flush();
    }

    @Test
    void findByCardIdOrderByReviewedAtDesc_returnsSortedLogs() {
        ReviewLog log1 = new ReviewLog(card, user, 3, 1000L, 2);
        ReviewLog log2 = new ReviewLog(card, user, 5, 800L, 3);
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.flush();

        List<ReviewLog> result = reviewLogRepository.findByCardIdOrderByReviewedAtDesc(card.getId());

        assertFalse(result.isEmpty());
    }

    @Test
    void countByUserIdAndReviewedAtBetween_countsCorrectly() {
        Instant now = Instant.now();
        Instant dayStart = now.minus(1, ChronoUnit.HOURS);
        Instant dayEnd = now.plus(1, ChronoUnit.HOURS);

        entityManager.persist(new ReviewLog(card, user, 4, null, null));
        entityManager.persist(new ReviewLog(card, user, 3, null, null));
        entityManager.flush();

        long count = reviewLogRepository.countByUserIdAndReviewedAtBetween(user.getId(), dayStart, dayEnd);
        assertEquals(2, count);
    }

    @Test
    void countPassedByUserInRange_countsOnlyPassing() {
        Instant now = Instant.now();
        Instant dayStart = now.minus(1, ChronoUnit.HOURS);
        Instant dayEnd = now.plus(1, ChronoUnit.HOURS);

        entityManager.persist(new ReviewLog(card, user, 4, null, null)); // passed
        entityManager.persist(new ReviewLog(card, user, 2, null, null)); // failed
        entityManager.persist(new ReviewLog(card, user, 3, null, null)); // passed
        entityManager.flush();

        long count = reviewLogRepository.countPassedByUserInRange(user.getId(), dayStart, dayEnd);
        assertEquals(2, count);
    }

    @Test
    void findAllByUserIdOrdered_returnsAscending() {
        entityManager.persist(new ReviewLog(card, user, 3, null, null));
        entityManager.persist(new ReviewLog(card, user, 4, null, null));
        entityManager.flush();

        List<ReviewLog> result = reviewLogRepository.findAllByUserIdOrdered(user.getId());

        assertEquals(2, result.size());
        // Should be ascending by reviewedAt
        assertTrue(result.get(0).getReviewedAt().compareTo(result.get(1).getReviewedAt()) <= 0);
    }

    @Test
    void findFragileReviews_findsCorrectButLowConfidence() {
        entityManager.persist(new ReviewLog(card, user, 4, null, 1)); // fragile: passed + low confidence
        entityManager.persist(new ReviewLog(card, user, 2, null, 1)); // not fragile: failed
        entityManager.persist(new ReviewLog(card, user, 5, null, 3)); // not fragile: high confidence
        entityManager.flush();

        List<ReviewLog> result = reviewLogRepository.findFragileReviews(user.getId());

        assertEquals(1, result.size());
        assertEquals(4, result.get(0).getRating());
        assertEquals(1, result.get(0).getConfidence());
    }

    @Test
    void findByCardIdAndUserIdOrderByReviewedAtDesc_returnsCorrectLogs() {
        User otherUser = new User("other@example.com", "hashed2");
        entityManager.persist(otherUser);

        entityManager.persist(new ReviewLog(card, user, 4, null, null));
        entityManager.persist(new ReviewLog(card, user, 3, null, null));
        entityManager.flush();

        List<ReviewLog> result = reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(card.getId(), user.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByCardIdAndUserIdOrderByReviewedAtDesc_isolatesUsers() {
        entityManager.persist(new ReviewLog(card, user, 4, null, null));
        entityManager.flush();

        User otherUser = new User("other@example.com", "hashed2");
        entityManager.persist(otherUser);
        entityManager.flush();

        List<ReviewLog> result = reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(
                card.getId(), otherUser.getId());

        assertTrue(result.isEmpty());
    }
}
