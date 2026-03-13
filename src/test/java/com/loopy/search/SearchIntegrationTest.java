package com.loopy.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.entity.CardType;
import com.loopy.topic.dto.CreateTopicRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the search endpoint — full Spring context with H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String auth;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("search-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        auth = "Bearer " + objectMapper.readValue(
                authResult.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    // ── Helper methods ──────────────────────────────────────────────

    private UUID createTopic(String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest(name, description, null))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createConcept(UUID topicId, String title, String notes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, title, notes))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createCard(UUID conceptId, String front, String back, String hint) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, front, back, CardType.STANDARD, hint, null))))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        return "Bearer " + objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    // ── Tests ────────────────────────────────────────────────────────

    @Test
    void search_findsTopic_byName() throws Exception {
        createTopic("Spring Boot Mastery", "Learn Spring Boot");

        mockMvc.perform(get("/api/search")
                        .param("q", "spring")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("Spring Boot Mastery"))
                .andExpect(jsonPath("$.topics[0].description").value("Learn Spring Boot"))
                .andExpect(jsonPath("$.topics[0].colorHex").exists());
    }

    @Test
    void search_findsTopic_byDescription() throws Exception {
        createTopic("Backend", "Covers REST APIs and microservices");

        mockMvc.perform(get("/api/search")
                        .param("q", "microservices")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("Backend"));
    }

    @Test
    void search_findsConcept_withTopicBreadcrumb() throws Exception {
        UUID topicId = createTopic("Java Basics", null);
        createConcept(topicId, "Polymorphism", "Runtime dispatch");

        mockMvc.perform(get("/api/search")
                        .param("q", "polymorphism")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concepts[0].title").value("Polymorphism"))
                .andExpect(jsonPath("$.concepts[0].notes").value("Runtime dispatch"))
                .andExpect(jsonPath("$.concepts[0].topicName").value("Java Basics"))
                .andExpect(jsonPath("$.concepts[0].topicId").value(topicId.toString()))
                .andExpect(jsonPath("$.concepts[0].status").value("LEARNING"));
    }

    @Test
    void search_findsCard_withConceptAndTopicBreadcrumbs() throws Exception {
        UUID topicId = createTopic("Data Structures", null);
        UUID conceptId = createConcept(topicId, "Binary Trees", null);
        createCard(conceptId, "What is a BST?", "A binary search tree", "Think sorted");

        mockMvc.perform(get("/api/search")
                        .param("q", "BST")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].front").value("What is a BST?"))
                .andExpect(jsonPath("$.cards[0].back").value("A binary search tree"))
                .andExpect(jsonPath("$.cards[0].hint").value("Think sorted"))
                .andExpect(jsonPath("$.cards[0].cardType").value("STANDARD"))
                .andExpect(jsonPath("$.cards[0].conceptTitle").value("Binary Trees"))
                .andExpect(jsonPath("$.cards[0].conceptId").value(conceptId.toString()))
                .andExpect(jsonPath("$.cards[0].topicName").value("Data Structures"))
                .andExpect(jsonPath("$.cards[0].topicId").value(topicId.toString()));
    }

    @Test
    void search_findsCard_byHint() throws Exception {
        UUID topicId = createTopic("Algorithms", null);
        UUID conceptId = createConcept(topicId, "Sorting", null);
        createCard(conceptId, "Name a stable sort", "Merge sort", "Think divide and conquer");

        mockMvc.perform(get("/api/search")
                        .param("q", "divide and conquer")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].front").value("Name a stable sort"));
    }

    @Test
    void search_combinedResults_acrossAllTypes() throws Exception {
        UUID topicId = createTopic("Kotlin Programming", "Kotlin language");
        UUID conceptId = createConcept(topicId, "Kotlin Coroutines", "Async in Kotlin");
        createCard(conceptId, "Kotlin suspend?", "Pauses coroutine", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "kotlin")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isNotEmpty())
                .andExpect(jsonPath("$.concepts").isNotEmpty())
                .andExpect(jsonPath("$.cards").isNotEmpty());
    }

    @Test
    void search_noMatches_returnsEmptyLists() throws Exception {
        createTopic("Something Else", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "xyznonexistent999")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_emptyQuery_returnsEmptyLists() throws Exception {
        createTopic("Should Not Appear", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_missingQueryParam_returnsEmptyLists() throws Exception {
        createTopic("Should Not Appear Either", null);

        mockMvc.perform(get("/api/search")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_whitespaceOnlyQuery_returnsEmptyLists() throws Exception {
        createTopic("Invisible Topic", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "   ")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_caseInsensitive() throws Exception {
        createTopic("PostgreSQL Tips", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "POSTGRESQL")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("PostgreSQL Tips"));
    }

    @Test
    void search_partialMatch() throws Exception {
        createTopic("Authentication Flow", null);

        mockMvc.perform(get("/api/search")
                        .param("q", "enti")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("Authentication Flow"));
    }

    @Test
    void search_ownershipIsolation() throws Exception {
        // User 1 creates content
        createTopic("User1 Secret Topic", null);

        // User 2 searches — should not see User 1's topic
        String auth2 = registerAndGetToken("search-other-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(get("/api/search")
                        .param("q", "User1 Secret")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "anything"))
                .andExpect(status().isForbidden());
    }
}
