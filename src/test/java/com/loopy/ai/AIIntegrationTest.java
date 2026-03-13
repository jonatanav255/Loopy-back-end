package com.loopy.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.ai.dto.EvaluateTeachBackRequest;
import com.loopy.ai.dto.GenerateCardsRequest;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateConceptRequest;
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
class AIIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String auth;
    private UUID conceptId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("ai-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        auth = "Bearer " + objectMapper.readValue(
                authResult.getResponse().getContentAsString(), TokenResponse.class).accessToken();

        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("AI Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID topicId = UUID.fromString(objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, "AI Concept", "notes"))))
                .andExpect(status().isCreated())
                .andReturn();
        conceptId = UUID.fromString(objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void statusEndpoint_returnsAvailability() throws Exception {
        // In test environment, API key is empty so AI should not be available
        mockMvc.perform(get("/api/ai/status")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void generateCards_failsWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/ai/generate-cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GenerateCardsRequest(conceptId, "Some learning content", 3))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void evaluateTeachBack_failsWithoutApiKey() throws Exception {
        mockMvc.perform(post("/api/ai/evaluate-teach-back")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EvaluateTeachBackRequest(conceptId, "My explanation"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generateCards_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/ai/generate-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new GenerateCardsRequest(conceptId, "content", 3))))
                .andExpect(status().isForbidden());
    }
}
