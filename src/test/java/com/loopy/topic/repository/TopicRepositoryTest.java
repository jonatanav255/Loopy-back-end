package com.loopy.topic.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
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
 * Repository tests for TopicRepository using @DataJpaTest.
 * Tests custom query methods, pagination, and JPA mappings.
 */
@DataJpaTest
class TopicRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TopicRepository topicRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@example.com", "hashed1");
        entityManager.persist(user1);

        user2 = new User("user2@example.com", "hashed2");
        entityManager.persist(user2);

        entityManager.flush();
    }

    @Test
    void findByUserIdOrderBySortOrderAsc_returnsTopicsSorted() {
        Topic topic2 = new Topic(user1, "B Topic", null, "#FF0000");
        topic2.setSortOrder(2);
        entityManager.persist(topic2);

        Topic topic1 = new Topic(user1, "A Topic", null, "#00FF00");
        topic1.setSortOrder(1);
        entityManager.persist(topic1);

        entityManager.flush();

        List<Topic> result = topicRepository.findByUserIdOrderBySortOrderAsc(user1.getId());

        assertEquals(2, result.size());
        assertEquals("A Topic", result.get(0).getName());
        assertEquals("B Topic", result.get(1).getName());
    }

    @Test
    void findByUserIdOrderBySortOrderAsc_isolatesUsers() {
        entityManager.persist(new Topic(user1, "User1 Topic", null, "#FF0000"));
        entityManager.persist(new Topic(user2, "User2 Topic", null, "#00FF00"));
        entityManager.flush();

        List<Topic> user1Topics = topicRepository.findByUserIdOrderBySortOrderAsc(user1.getId());
        List<Topic> user2Topics = topicRepository.findByUserIdOrderBySortOrderAsc(user2.getId());

        assertEquals(1, user1Topics.size());
        assertEquals("User1 Topic", user1Topics.get(0).getName());
        assertEquals(1, user2Topics.size());
        assertEquals("User2 Topic", user2Topics.get(0).getName());
    }

    @Test
    void findByIdAndUserId_existingTopicOwnedByUser_returns() {
        Topic topic = new Topic(user1, "My Topic", null, "#FF0000");
        entityManager.persist(topic);
        entityManager.flush();

        Optional<Topic> found = topicRepository.findByIdAndUserId(topic.getId(), user1.getId());
        assertTrue(found.isPresent());
        assertEquals("My Topic", found.get().getName());
    }

    @Test
    void findByIdAndUserId_wrongUser_returnsEmpty() {
        Topic topic = new Topic(user1, "My Topic", null, "#FF0000");
        entityManager.persist(topic);
        entityManager.flush();

        Optional<Topic> found = topicRepository.findByIdAndUserId(topic.getId(), user2.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void findByIdAndUserId_nonExistent_returnsEmpty() {
        Optional<Topic> found = topicRepository.findByIdAndUserId(UUID.randomUUID(), user1.getId());
        assertTrue(found.isEmpty());
    }

    @Test
    void findAllByUserIdAndIdIn_returnsOnlyMatchingOwnedTopics() {
        Topic t1 = new Topic(user1, "Topic A", null, "#FF0000");
        Topic t2 = new Topic(user1, "Topic B", null, "#00FF00");
        Topic t3 = new Topic(user2, "Other User Topic", null, "#0000FF");

        entityManager.persist(t1);
        entityManager.persist(t2);
        entityManager.persist(t3);
        entityManager.flush();

        List<Topic> result = topicRepository.findAllByUserIdAndIdIn(
                user1.getId(), List.of(t1.getId(), t2.getId(), t3.getId()));

        assertEquals(2, result.size());
    }

    @Test
    void searchByUser_matchesNameCaseInsensitive() {
        entityManager.persist(new Topic(user1, "Java Programming", "Learn Java", "#FF0000"));
        entityManager.persist(new Topic(user1, "Python", "Learn Python", "#00FF00"));
        entityManager.flush();

        List<Topic> result = topicRepository.searchByUser(user1.getId(), "%java%");

        assertEquals(1, result.size());
        assertEquals("Java Programming", result.get(0).getName());
    }

    @Test
    void searchByUser_matchesDescription() {
        entityManager.persist(new Topic(user1, "Web Dev", "Learn JavaScript and HTML", "#FF0000"));
        entityManager.flush();

        List<Topic> result = topicRepository.searchByUser(user1.getId(), "%JavaScript%");

        assertEquals(1, result.size());
        assertEquals("Web Dev", result.get(0).getName());
    }

    @Test
    void searchByUser_noMatches_returnsEmpty() {
        entityManager.persist(new Topic(user1, "Java", "desc", "#FF0000"));
        entityManager.flush();

        List<Topic> result = topicRepository.searchByUser(user1.getId(), "%nonexistent%");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchByUser_doesNotReturnOtherUsersTopics() {
        entityManager.persist(new Topic(user1, "Java", null, "#FF0000"));
        entityManager.persist(new Topic(user2, "Java Advanced", null, "#00FF00"));
        entityManager.flush();

        List<Topic> result = topicRepository.searchByUser(user1.getId(), "%Java%");

        assertEquals(1, result.size());
    }
}
