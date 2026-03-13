package com.loopy.topic;

import com.loopy.auth.entity.User;
import com.loopy.card.repository.CardRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.config.ReorderRequest;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.TopicResponse;
import com.loopy.topic.dto.UpdateTopicRequest;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import com.loopy.topic.service.TopicService;
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
 * Unit tests for TopicService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private CardRepository cardRepository;

    private TopicService topicService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        topicService = new TopicService(topicRepository, cardRepository);
        testUser = new User("user@example.com", "hashed");
        // Use reflection to set the ID since it's auto-generated
        userId = UUID.randomUUID();
        setId(testUser, userId);
    }

    @Test
    void createTopic_savesAndReturnsResponse() {
        CreateTopicRequest request = new CreateTopicRequest("Java Basics", "Learn Java", "#FF0000");

        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            setId(topic, UUID.randomUUID());
            return topic;
        });

        TopicResponse response = topicService.createTopic(request, testUser);

        assertNotNull(response);
        assertEquals("Java Basics", response.name());
        assertEquals("Learn Java", response.description());
        assertEquals("#FF0000", response.colorHex());

        verify(topicRepository).save(any(Topic.class));
    }

    @Test
    void createTopic_defaultColor_whenNotProvided() {
        CreateTopicRequest request = new CreateTopicRequest("No Color", "Desc", null);

        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            setId(topic, UUID.randomUUID());
            return topic;
        });

        TopicResponse response = topicService.createTopic(request, testUser);

        assertEquals("#6366F1", response.colorHex());
    }

    @Test
    void getTopics_returnsOnlyUserTopics() {
        Topic topic1 = new Topic(testUser, "Topic 1", "Desc 1", "#111111");
        Topic topic2 = new Topic(testUser, "Topic 2", "Desc 2", "#222222");
        setId(topic1, UUID.randomUUID());
        setId(topic2, UUID.randomUUID());

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId)).thenReturn(List.of(topic1, topic2));
        when(cardRepository.countByTopicId(any())).thenReturn(0L);

        List<TopicResponse> topics = topicService.getTopics(testUser);

        assertEquals(2, topics.size());
        assertEquals("Topic 1", topics.get(0).name());
        assertEquals("Topic 2", topics.get(1).name());

        verify(topicRepository).findByUserIdOrderBySortOrderAsc(userId);
    }

    @Test
    void getTopic_found_returnsResponse() {
        UUID topicId = UUID.randomUUID();
        Topic topic = new Topic(testUser, "Found Topic", "Desc", "#333333");
        setId(topic, topicId);

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(topic));

        TopicResponse response = topicService.getTopic(topicId, testUser);

        assertEquals("Found Topic", response.name());
        assertEquals(topicId, response.id());
    }

    @Test
    void getTopic_notFound_throwsResourceNotFoundException() {
        UUID topicId = UUID.randomUUID();
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> topicService.getTopic(topicId, testUser));
    }

    @Test
    void updateTopic_updatesFieldsCorrectly() {
        UUID topicId = UUID.randomUUID();
        Topic existingTopic = new Topic(testUser, "Old Name", "Old Desc", "#000000");
        setId(existingTopic, topicId);

        UpdateTopicRequest request = new UpdateTopicRequest("New Name", "New Desc", "#FFFFFF");

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(existingTopic));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TopicResponse response = topicService.updateTopic(topicId, request, testUser);

        assertEquals("New Name", response.name());
        assertEquals("New Desc", response.description());
        assertEquals("#FFFFFF", response.colorHex());

        verify(topicRepository).save(existingTopic);
    }

    @Test
    void updateTopic_nullColor_keepsPreviousColor() {
        UUID topicId = UUID.randomUUID();
        Topic existingTopic = new Topic(testUser, "Old Name", "Old Desc", "#AABBCC");
        setId(existingTopic, topicId);

        UpdateTopicRequest request = new UpdateTopicRequest("Updated", "Updated Desc", null);

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(existingTopic));
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TopicResponse response = topicService.updateTopic(topicId, request, testUser);

        // Color should remain unchanged since request.colorHex() is null
        assertEquals("#AABBCC", response.colorHex());
    }

    @Test
    void deleteTopic_callsRepositoryDelete() {
        UUID topicId = UUID.randomUUID();
        Topic topic = new Topic(testUser, "To Delete", "Desc", "#000000");
        setId(topic, topicId);

        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.of(topic));

        topicService.deleteTopic(topicId, testUser);

        verify(topicRepository).delete(topic);
    }

    @Test
    void deleteTopic_wrongUser_throwsException() {
        UUID topicId = UUID.randomUUID();
        // Topic not found for this user (belongs to someone else)
        when(topicRepository.findByIdAndUserId(topicId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> topicService.deleteTopic(topicId, testUser));

        verify(topicRepository, never()).delete(any());
    }

    @Test
    void reorderTopics_updatesSortOrderCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Topic t1 = new Topic(testUser, "Topic 1", "Desc 1", "#111111");
        Topic t2 = new Topic(testUser, "Topic 2", "Desc 2", "#222222");
        Topic t3 = new Topic(testUser, "Topic 3", "Desc 3", "#333333");
        setId(t1, id1);
        setId(t2, id2);
        setId(t3, id3);

        // Request reorder: t3 first, t1 second, t2 third
        List<UUID> orderedIds = List.of(id3, id1, id2);
        ReorderRequest request = new ReorderRequest(orderedIds);

        when(topicRepository.findAllByUserIdAndIdIn(userId, orderedIds))
                .thenReturn(List.of(t1, t2, t3));
        when(topicRepository.saveAll(any())).thenReturn(List.of(t3, t1, t2));
        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId))
                .thenReturn(List.of(t3, t1, t2));
        when(cardRepository.countByTopicId(any())).thenReturn(0L);

        List<TopicResponse> result = topicService.reorderTopics(request, testUser);

        // Verify sort orders were set based on position in orderedIds
        assertEquals(2, t1.getSortOrder()); // id1 is at index 1 → sortOrder 2
        assertEquals(3, t2.getSortOrder()); // id2 is at index 2 → sortOrder 3
        assertEquals(1, t3.getSortOrder()); // id3 is at index 0 → sortOrder 1

        verify(topicRepository).saveAll(any());
        assertEquals(3, result.size());
    }

    @Test
    void reorderTopics_missingTopic_throwsException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Topic t1 = new Topic(testUser, "Topic 1", "Desc 1", "#111111");
        Topic t2 = new Topic(testUser, "Topic 2", "Desc 2", "#222222");
        setId(t1, id1);
        setId(t2, id2);

        // Request has 3 IDs but only 2 topics exist
        List<UUID> orderedIds = List.of(id1, id2, id3);
        ReorderRequest request = new ReorderRequest(orderedIds);

        when(topicRepository.findAllByUserIdAndIdIn(userId, orderedIds))
                .thenReturn(List.of(t1, t2));

        assertThrows(ResourceNotFoundException.class,
                () -> topicService.reorderTopics(request, testUser));

        verify(topicRepository, never()).saveAll(any());
    }

    @Test
    void createTopic_assignsNextSortOrder() {
        Topic existing1 = new Topic(testUser, "Existing 1", "Desc", "#111111");
        Topic existing2 = new Topic(testUser, "Existing 2", "Desc", "#222222");
        setId(existing1, UUID.randomUUID());
        setId(existing2, UUID.randomUUID());
        existing1.setSortOrder(1);
        existing2.setSortOrder(2);

        when(topicRepository.findByUserIdOrderBySortOrderAsc(userId))
                .thenReturn(List.of(existing1, existing2));

        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        when(topicRepository.save(topicCaptor.capture())).thenAnswer(invocation -> {
            Topic t = invocation.getArgument(0);
            setId(t, UUID.randomUUID());
            return t;
        });

        CreateTopicRequest request = new CreateTopicRequest("New Topic", "New Desc", "#AABBCC");
        topicService.createTopic(request, testUser);

        Topic savedTopic = topicCaptor.getValue();
        assertEquals(3, savedTopic.getSortOrder());
        assertEquals("New Topic", savedTopic.getName());
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
