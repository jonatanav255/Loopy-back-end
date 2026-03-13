package com.loopy.search;

import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.search.dto.SearchResponse;
import com.loopy.search.service.SearchService;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SearchService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ConceptRepository conceptRepository;

    @Mock
    private CardRepository cardRepository;

    private SearchService searchService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(topicRepository, conceptRepository, cardRepository);
        testUser = new User("user@example.com", "hashed");
        userId = UUID.randomUUID();
        setId(testUser, userId);
    }

    @Test
    void search_returnsMatchingTopics() {
        Topic topic = new Topic(testUser, "Java Basics", "Learn Java fundamentals", "#FF0000");
        setId(topic, UUID.randomUUID());

        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(topic));
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("java", testUser);

        assertEquals(1, response.topics().size());
        assertEquals("Java Basics", response.topics().get(0).name());
        assertEquals("Learn Java fundamentals", response.topics().get(0).description());
        assertEquals("#FF0000", response.topics().get(0).colorHex());
        assertTrue(response.concepts().isEmpty());
        assertTrue(response.cards().isEmpty());
    }

    @Test
    void search_returnsMatchingConcepts_withTopicContext() {
        Topic topic = new Topic(testUser, "Java Basics", "Desc", "#FF0000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "Inheritance", "OOP concept");
        setId(concept, UUID.randomUUID());

        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(concept));
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("inherit", testUser);

        assertEquals(1, response.concepts().size());
        assertEquals("Inheritance", response.concepts().get(0).title());
        assertEquals("OOP concept", response.concepts().get(0).notes());
        assertEquals(topic.getId(), response.concepts().get(0).topicId());
        assertEquals("Java Basics", response.concepts().get(0).topicName());
        assertEquals(ConceptStatus.LEARNING.name(), response.concepts().get(0).status());
    }

    @Test
    void search_returnsMatchingCards_withConceptAndTopicContext() {
        Topic topic = new Topic(testUser, "Java Basics", "Desc", "#FF0000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "Inheritance", "Notes");
        setId(concept, UUID.randomUUID());
        Card card = new Card(concept, testUser, "What is inheritance?", "A mechanism for code reuse",
                CardType.STANDARD, "Think OOP", null);
        setId(card, UUID.randomUUID());

        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(card));

        SearchResponse response = searchService.search("inheritance", testUser);

        assertEquals(1, response.cards().size());
        assertEquals("What is inheritance?", response.cards().get(0).front());
        assertEquals("A mechanism for code reuse", response.cards().get(0).back());
        assertEquals("Think OOP", response.cards().get(0).hint());
        assertEquals(CardType.STANDARD.name(), response.cards().get(0).cardType());
        assertEquals(concept.getId(), response.cards().get(0).conceptId());
        assertEquals("Inheritance", response.cards().get(0).conceptTitle());
        assertEquals(topic.getId(), response.cards().get(0).topicId());
        assertEquals("Java Basics", response.cards().get(0).topicName());
    }

    @Test
    void search_combinedResults_allTypesReturned() {
        Topic topic = new Topic(testUser, "Java Basics", "Desc", "#FF0000");
        setId(topic, UUID.randomUUID());
        Concept concept = new Concept(topic, testUser, "Java Inheritance", "Notes");
        setId(concept, UUID.randomUUID());
        Card card = new Card(concept, testUser, "Java question", "Java answer",
                CardType.STANDARD, null, null);
        setId(card, UUID.randomUUID());

        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(topic));
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(concept));
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(List.of(card));

        SearchResponse response = searchService.search("java", testUser);

        assertEquals(1, response.topics().size());
        assertEquals(1, response.concepts().size());
        assertEquals(1, response.cards().size());
    }

    @Test
    void search_noMatches_returnsEmptyLists() {
        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("zzzznothing", testUser);

        assertTrue(response.topics().isEmpty());
        assertTrue(response.concepts().isEmpty());
        assertTrue(response.cards().isEmpty());
    }

    @Test
    void search_limitsResultsPerType() {
        List<Topic> fifteenTopics = IntStream.range(0, 15)
                .mapToObj(i -> {
                    Topic t = new Topic(testUser, "Topic " + i, "Desc", "#000000");
                    setId(t, UUID.randomUUID());
                    return t;
                })
                .toList();

        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(fifteenTopics);
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("topic", testUser);

        assertEquals(10, response.topics().size());
    }

    @Test
    void search_passesCorrectPatternToRepositories() {
        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        searchService.search("java", testUser);

        ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);

        verify(topicRepository).searchByUser(eq(userId), patternCaptor.capture());
        assertEquals("%java%", patternCaptor.getValue());

        verify(conceptRepository).searchByUser(eq(userId), patternCaptor.capture());
        assertEquals("%java%", patternCaptor.getValue());

        verify(cardRepository).searchByUser(eq(userId), patternCaptor.capture());
        assertEquals("%java%", patternCaptor.getValue());
    }

    @Test
    void search_usesCorrectUserId() {
        when(topicRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(conceptRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());
        when(cardRepository.searchByUser(eq(userId), anyString())).thenReturn(Collections.emptyList());

        searchService.search("test", testUser);

        verify(topicRepository).searchByUser(eq(userId), anyString());
        verify(conceptRepository).searchByUser(eq(userId), anyString());
        verify(cardRepository).searchByUser(eq(userId), anyString());
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
