package com.loopy.card.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for CardRepository using @DataJpaTest.
 */
@DataJpaTest
class CardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardRepository cardRepository;

    private User user1;
    private User user2;
    private Topic topic1;
    private Topic topic2;
    private Concept concept1;
    private Concept concept2;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "hashed1");
        entityManager.persist(user1);

        user2 = new User("user2@example.com", "hashed2");
        entityManager.persist(user2);

        topic1 = new Topic(user1, "Java", null, "#FF0000");
        entityManager.persist(topic1);

        topic2 = new Topic(user1, "Python", null, "#00FF00");
        entityManager.persist(topic2);

        concept1 = new Concept(topic1, user1, "OOP", null);
        entityManager.persist(concept1);

        concept2 = new Concept(topic2, user1, "Data Types", null);
        entityManager.persist(concept2);

        entityManager.flush();
    }

    @Test
    void findByConceptIdAndUserIdOrderByCreatedAtDesc_returnsCards() {
        Card card1 = new Card(concept1, user1, "Q1", "A1", CardType.STANDARD, null, null);
        Card card2 = new Card(concept1, user1, "Q2", "A2", CardType.STANDARD, null, null);
        entityManager.persist(card1);
        entityManager.persist(card2);
        entityManager.flush();

        List<Card> result = cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(concept1.getId(), user1.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByIdAndUserId_correctOwner_returns() {
        Card card = new Card(concept1, user1, "Q", "A", CardType.STANDARD, null, null);
        entityManager.persist(card);
        entityManager.flush();

        Optional<Card> found = cardRepository.findByIdAndUserId(card.getId(), user1.getId());
        assertTrue(found.isPresent());
        assertEquals("Q", found.get().getFront());
    }

    @Test
    void findByIdAndUserId_wrongOwner_returnsEmpty() {
        Card card = new Card(concept1, user1, "Q", "A", CardType.STANDARD, null, null);
        entityManager.persist(card);
        entityManager.flush();

        Optional<Card> found = cardRepository.findByIdAndUserId(card.getId(), user2.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void findDueCards_returnsDueCardsOrdered() {
        Card dueCard = new Card(concept1, user1, "Due Q", "Due A", CardType.STANDARD, null, null);
        dueCard.setNextReviewDate(LocalDate.now().minusDays(1));
        dueCard.setEaseFactor(2.0);
        entityManager.persist(dueCard);

        Card futureCard = new Card(concept1, user1, "Future Q", "Future A", CardType.STANDARD, null, null);
        futureCard.setNextReviewDate(LocalDate.now().plusDays(5));
        entityManager.persist(futureCard);

        Card todayCard = new Card(concept1, user1, "Today Q", "Today A", CardType.STANDARD, null, null);
        todayCard.setNextReviewDate(LocalDate.now());
        todayCard.setEaseFactor(2.5);
        entityManager.persist(todayCard);

        entityManager.flush();

        List<Card> result = cardRepository.findDueCards(user1.getId(), LocalDate.now());

        assertEquals(2, result.size());
        // Should be sorted by nextReviewDate ASC, then easeFactor ASC
        assertEquals("Due Q", result.get(0).getFront());
    }

    @Test
    void findDueCardsByTopics_filtersTopics() {
        Card card1 = new Card(concept1, user1, "Java Q", "Java A", CardType.STANDARD, null, null);
        card1.setNextReviewDate(LocalDate.now());
        entityManager.persist(card1);

        Card card2 = new Card(concept2, user1, "Python Q", "Python A", CardType.STANDARD, null, null);
        card2.setNextReviewDate(LocalDate.now());
        entityManager.persist(card2);

        entityManager.flush();

        List<Card> result = cardRepository.findDueCardsByTopics(
                user1.getId(), LocalDate.now(), List.of(topic1.getId()));

        assertEquals(1, result.size());
        assertEquals("Java Q", result.get(0).getFront());
    }

    @Test
    void countByTopicId_countsCorrectly() {
        entityManager.persist(new Card(concept1, user1, "Q1", "A1", CardType.STANDARD, null, null));
        entityManager.persist(new Card(concept1, user1, "Q2", "A2", CardType.STANDARD, null, null));
        entityManager.flush();

        long count = cardRepository.countByTopicId(topic1.getId());
        assertEquals(2, count);
    }

    @Test
    void countDueCards_countsCorrectly() {
        Card dueCard = new Card(concept1, user1, "Q", "A", CardType.STANDARD, null, null);
        dueCard.setNextReviewDate(LocalDate.now());
        entityManager.persist(dueCard);

        Card futureCard = new Card(concept1, user1, "Q2", "A2", CardType.STANDARD, null, null);
        futureCard.setNextReviewDate(LocalDate.now().plusDays(3));
        entityManager.persist(futureCard);

        entityManager.flush();

        long count = cardRepository.countDueCards(user1.getId(), LocalDate.now());
        assertEquals(1, count);
    }

    @Test
    void countByUserId_countsAllCards() {
        entityManager.persist(new Card(concept1, user1, "Q1", "A1", CardType.STANDARD, null, null));
        entityManager.persist(new Card(concept2, user1, "Q2", "A2", CardType.STANDARD, null, null));
        entityManager.flush();

        long count = cardRepository.countByUserId(user1.getId());
        assertEquals(2, count);
    }

    @Test
    void findCardsByTopics_filtersAndOrdersByCreatedAtDesc() {
        entityManager.persist(new Card(concept1, user1, "Java Q", "A", CardType.STANDARD, null, null));
        entityManager.persist(new Card(concept2, user1, "Python Q", "A", CardType.STANDARD, null, null));
        entityManager.flush();

        List<Card> result = cardRepository.findCardsByTopics(user1.getId(), List.of(topic1.getId()));

        assertEquals(1, result.size());
        assertEquals("Java Q", result.get(0).getFront());
    }

    @Test
    void searchByUser_matchesFront() {
        entityManager.persist(new Card(concept1, user1, "What is polymorphism?", "A concept in OOP", CardType.STANDARD, null, null));
        entityManager.persist(new Card(concept1, user1, "What is inheritance?", "Parent-child classes", CardType.STANDARD, null, null));
        entityManager.flush();

        List<Card> result = cardRepository.searchByUser(user1.getId(), "%polymorphism%");

        assertEquals(1, result.size());
        assertEquals("What is polymorphism?", result.get(0).getFront());
    }

    @Test
    void searchByUser_matchesBack() {
        entityManager.persist(new Card(concept1, user1, "What is OOP?", "Object-Oriented Programming", CardType.STANDARD, null, null));
        entityManager.flush();

        List<Card> result = cardRepository.searchByUser(user1.getId(), "%Object-Oriented%");

        assertEquals(1, result.size());
    }

    @Test
    void searchByUser_matchesHint() {
        entityManager.persist(new Card(concept1, user1, "Q", "A", CardType.STANDARD, "Think about encapsulation", null));
        entityManager.flush();

        List<Card> result = cardRepository.searchByUser(user1.getId(), "%encapsulation%");

        assertEquals(1, result.size());
    }

    @Test
    void searchByUser_isolatesUsers() {
        Topic otherTopic = new Topic(user2, "Other", null, "#0000FF");
        entityManager.persist(otherTopic);
        Concept otherC = new Concept(otherTopic, user2, "Other", null);
        entityManager.persist(otherC);

        entityManager.persist(new Card(concept1, user1, "Shared keyword", "A", CardType.STANDARD, null, null));
        entityManager.persist(new Card(otherC, user2, "Shared keyword", "A", CardType.STANDARD, null, null));
        entityManager.flush();

        List<Card> result = cardRepository.searchByUser(user1.getId(), "%Shared%");

        assertEquals(1, result.size());
    }
}
