package com.loopy.card;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateCardRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String auth;
    private UUID topicId;
    private UUID conceptId;

    @BeforeEach
    void setUp() throws Exception {
        // Register user
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("card-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        auth = "Bearer " + objectMapper.readValue(
                authResult.getResponse().getContentAsString(), TokenResponse.class).accessToken();

        // Create topic
        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("Test Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        topicId = UUID.fromString(objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText());

        // Create concept
        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, "Test Concept", "Some notes"))))
                .andExpect(status().isCreated())
                .andReturn();

        conceptId = UUID.fromString(objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void conceptCrud() throws Exception {
        // List concepts by topic
        mockMvc.perform(get("/api/concepts")
                        .param("topicId", topicId.toString())
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Concept"));

        // Get single concept
        mockMvc.perform(get("/api/concepts/" + conceptId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Concept"));
    }

    @Test
    void cardCrud() throws Exception {
        // Create card
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, "Q: What is JPA?",
                                        "A: Java Persistence API", CardType.STANDARD, "Think ORM", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.front").value("Q: What is JPA?"))
                .andExpect(jsonPath("$.cardType").value("STANDARD"))
                .andExpect(jsonPath("$.hint").value("Think ORM"))
                .andReturn();

        String cardId = objectMapper.readTree(
                cardResult.getResponse().getContentAsString()).get("id").asText();

        // List cards by concept
        mockMvc.perform(get("/api/cards")
                        .param("conceptId", conceptId.toString())
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("Q: What is JPA?"));

        // Update card
        mockMvc.perform(put("/api/cards/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateCardRequest("Updated Q", "Updated A",
                                        CardType.CODE_OUTPUT, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("Updated Q"))
                .andExpect(jsonPath("$.cardType").value("CODE_OUTPUT"));

        // Delete card
        mockMvc.perform(delete("/api/cards/" + cardId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/cards/" + cardId)
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotCreateCardForOtherUsersContent() throws Exception {
        // Register second user
        MvcResult authResult2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("card-other-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        String auth2 = "Bearer " + objectMapper.readValue(
                authResult2.getResponse().getContentAsString(), TokenResponse.class).accessToken();

        // User 2 tries to create a card under user 1's concept — should fail
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, "Hack", "Attack", null, null, null))))
                .andExpect(status().isNotFound());
    }
}
