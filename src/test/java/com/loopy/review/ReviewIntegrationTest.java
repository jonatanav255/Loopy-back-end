package com.loopy.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.topic.dto.CreateTopicRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullReviewFlow() throws Exception {
        // Register a user
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("review@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readValue(
                authResult.getResponse().getContentAsString(), TokenResponse.class).accessToken();
        String auth = "Bearer " + token;

        // Create topic
        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("Java", "Java basics", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andReturn();

        String topicId = objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText();

        // Create concept
        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(
                                        java.util.UUID.fromString(topicId),
                                        "Generics",
                                        "Type parameters in Java"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Generics"))
                .andReturn();

        String conceptId = objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText();

        // Create card
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(
                                        java.util.UUID.fromString(conceptId),
                                        "What is type erasure?",
                                        "Generics are removed at compile time",
                                        null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.front").value("What is type erasure?"))
                .andExpect(jsonPath("$.repetitionCount").value(0))
                .andExpect(jsonPath("$.easeFactor").value(2.5))
                .andReturn();

        String cardId = objectMapper.readTree(
                cardResult.getResponse().getContentAsString()).get("id").asText();

        // Verify card appears in today's due list (nextReviewDate defaults to today)
        mockMvc.perform(get("/api/reviews/today")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(cardId));

        // Submit review (rating 4 = good)
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 1500L, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.responseTimeMs").value(1500))
                .andExpect(jsonPath("$.updatedCard.repetitionCount").value(1))
                .andExpect(jsonPath("$.updatedCard.intervalDays").value(1));

        // Card should no longer be due today (nextReviewDate = tomorrow)
        mockMvc.perform(get("/api/reviews/today")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Verify card state was persisted
        mockMvc.perform(get("/api/cards/" + cardId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repetitionCount").value(1))
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.lastReviewDate").isNotEmpty());
    }
}
