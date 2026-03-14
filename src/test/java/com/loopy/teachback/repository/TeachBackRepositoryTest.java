package com.loopy.teachback.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Concept;
import com.loopy.teachback.entity.TeachBack;
import com.loopy.topic.entity.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for TeachBackRepository using @DataJpaTest.
 */
@DataJpaTest
class TeachBackRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeachBackRepository teachBackRepository;

    private User user;
    private Concept concept;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "hashed");
        entityManager.persist(user);

        Topic topic = new Topic(user, "Java", null, "#FF0000");
        entityManager.persist(topic);

        concept = new Concept(topic, user, "OOP", null);
        entityManager.persist(concept);

        entityManager.flush();
    }

    @Test
    void findByConceptIdAndUserIdOrderByCreatedAtDesc_returnsSorted() {
        entityManager.persist(new TeachBack(concept, user, "Explanation 1", 3, null));
        entityManager.persist(new TeachBack(concept, user, "Explanation 2", 4, "gap1|gap2"));
        entityManager.flush();

        List<TeachBack> result = teachBackRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(
                concept.getId(), user.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsAllForUser() {
        Topic topic2 = new Topic(user, "Python", null, "#00FF00");
        entityManager.persist(topic2);
        Concept concept2 = new Concept(topic2, user, "Data Types", null);
        entityManager.persist(concept2);

        entityManager.persist(new TeachBack(concept, user, "Explanation 1", 3, null));
        entityManager.persist(new TeachBack(concept2, user, "Explanation 2", 5, null));
        entityManager.flush();

        List<TeachBack> result = teachBackRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findByConceptIdAndUserIdOrderByCreatedAtDesc_isolatesUsers() {
        User user2 = new User("user2@example.com", "hashed2");
        entityManager.persist(user2);

        entityManager.persist(new TeachBack(concept, user, "User1 explanation", 3, null));
        entityManager.flush();

        List<TeachBack> result = teachBackRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(
                concept.getId(), user2.getId());

        assertTrue(result.isEmpty());
    }
}
