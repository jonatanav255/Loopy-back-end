package com.loopy.card.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for ConceptRepository using @DataJpaTest.
 */
@DataJpaTest
class ConceptRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConceptRepository conceptRepository;

    private User user1;
    private User user2;
    private Topic topic1;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "hashed1");
        entityManager.persist(user1);

        user2 = new User("user2@example.com", "hashed2");
        entityManager.persist(user2);

        topic1 = new Topic(user1, "Java", "desc", "#FF0000");
        entityManager.persist(topic1);

        entityManager.flush();
    }

    @Test
    void findByTopicIdAndUserIdOrderBySortOrderAsc_returnsSorted() {
        Concept c2 = new Concept(topic1, user1, "B Concept", null);
        c2.setSortOrder(2);
        entityManager.persist(c2);

        Concept c1 = new Concept(topic1, user1, "A Concept", null);
        c1.setSortOrder(1);
        entityManager.persist(c1);

        entityManager.flush();

        List<Concept> result = conceptRepository.findByTopicIdAndUserIdOrderBySortOrderAsc(topic1.getId(), user1.getId());

        assertEquals(2, result.size());
        assertEquals("A Concept", result.get(0).getTitle());
        assertEquals("B Concept", result.get(1).getTitle());
    }

    @Test
    void findByIdAndUserId_correctOwner_returns() {
        Concept concept = new Concept(topic1, user1, "OOP", "notes");
        entityManager.persist(concept);
        entityManager.flush();

        Optional<Concept> found = conceptRepository.findByIdAndUserId(concept.getId(), user1.getId());
        assertTrue(found.isPresent());
        assertEquals("OOP", found.get().getTitle());
    }

    @Test
    void findByIdAndUserId_wrongOwner_returnsEmpty() {
        Concept concept = new Concept(topic1, user1, "OOP", "notes");
        entityManager.persist(concept);
        entityManager.flush();

        Optional<Concept> found = conceptRepository.findByIdAndUserId(concept.getId(), user2.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void findWithTopicByIdAndUserId_eagerLoadsTopic() {
        Concept concept = new Concept(topic1, user1, "OOP", null);
        entityManager.persist(concept);
        entityManager.flush();
        entityManager.clear(); // force fresh load

        Optional<Concept> found = conceptRepository.findWithTopicByIdAndUserId(concept.getId(), user1.getId());
        assertTrue(found.isPresent());
        // Topic should be eagerly loaded — no LazyInitializationException
        assertNotNull(found.get().getTopic());
        assertEquals("Java", found.get().getTopic().getName());
    }

    @Test
    void findByUserIdAndStatusOrderByUpdatedAtDesc_filtersStatus() {
        Concept learning = new Concept(topic1, user1, "Learning Concept", null);
        entityManager.persist(learning);

        Concept teachBack = new Concept(topic1, user1, "TeachBack Concept", null);
        teachBack.setStatus(ConceptStatus.TEACH_BACK_REQUIRED);
        entityManager.persist(teachBack);

        entityManager.flush();

        List<Concept> result = conceptRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                user1.getId(), ConceptStatus.TEACH_BACK_REQUIRED);

        assertEquals(1, result.size());
        assertEquals("TeachBack Concept", result.get(0).getTitle());
    }

    @Test
    void findAllByUserIdAndIdIn_returnsOnlyOwnedConcepts() {
        Concept c1 = new Concept(topic1, user1, "C1", null);
        entityManager.persist(c1);

        Topic topic2 = new Topic(user2, "Other", null, "#00FF00");
        entityManager.persist(topic2);

        Concept c2 = new Concept(topic2, user2, "C2", null);
        entityManager.persist(c2);

        entityManager.flush();

        List<Concept> result = conceptRepository.findAllByUserIdAndIdIn(
                user1.getId(), List.of(c1.getId(), c2.getId()));

        assertEquals(1, result.size());
        assertEquals("C1", result.get(0).getTitle());
    }

    @Test
    void searchByUser_matchesTitleCaseInsensitive() {
        entityManager.persist(new Concept(topic1, user1, "Object-Oriented Programming", "OOP notes"));
        entityManager.persist(new Concept(topic1, user1, "Data Structures", "DS notes"));
        entityManager.flush();

        List<Concept> result = conceptRepository.searchByUser(user1.getId(), "%object%");

        assertEquals(1, result.size());
        assertEquals("Object-Oriented Programming", result.get(0).getTitle());
    }

    @Test
    void searchByUser_matchesNotes() {
        entityManager.persist(new Concept(topic1, user1, "Concept 1", "Learn about polymorphism"));
        entityManager.flush();

        List<Concept> result = conceptRepository.searchByUser(user1.getId(), "%polymorphism%");

        assertEquals(1, result.size());
    }

    @Test
    void searchByUser_doesNotReturnOtherUsersData() {
        Topic topic2 = new Topic(user2, "Other Topic", null, "#00FF00");
        entityManager.persist(topic2);

        entityManager.persist(new Concept(topic1, user1, "OOP", null));
        entityManager.persist(new Concept(topic2, user2, "OOP", null));
        entityManager.flush();

        List<Concept> result = conceptRepository.searchByUser(user1.getId(), "%OOP%");

        assertEquals(1, result.size());
    }
}
