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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndAuth() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("stats-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return "Bearer " + objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    private String createTopicConceptCard(String auth) throws Exception {
        // Create topic
        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("Stats Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID topicId = UUID.fromString(objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText());

        // Create concept
        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, "Stats Concept", "notes"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID conceptId = UUID.fromString(objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText());

        // Create card
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, "Q?", "A.", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                cardResult.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void overviewReturnsDefaults_whenNoReviews() throws Exception {
        String auth = registerAndAuth();

        mockMvc.perform(get("/api/stats/overview")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsDueToday").value(0))
                .andExpect(jsonPath("$.cardsReviewedToday").value(0))
                .andExpect(jsonPath("$.totalCards").value(0))
                .andExpect(jsonPath("$.accuracyToday").value(0.0))
                .andExpect(jsonPath("$.currentStreak").value(0))
                .andExpect(jsonPath("$.longestStreak").value(0));
    }

    @Test
    void overviewUpdatesAfterReview() throws Exception {
        String auth = registerAndAuth();
        String cardId = createTopicConceptCard(auth);

        // Submit review with confidence
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 1200L, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").value(3));

        // Overview should reflect the review
        mockMvc.perform(get("/api/stats/overview")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsReviewedToday").value(1))
                .andExpect(jsonPath("$.totalCards").value(1))
                .andExpect(jsonPath("$.accuracyToday").value(100.0))
                .andExpect(jsonPath("$.currentStreak").value(1));
    }

    @Test
    void heatmap_returnsEntriesAfterReview() throws Exception {
        String auth = registerAndAuth();
        String cardId = createTopicConceptCard(auth);

        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 800L, 2))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stats/heatmap")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(1));
    }

    @Test
    void fragileCards_detectsCorrectButLowConfidence() throws Exception {
        String auth = registerAndAuth();
        String cardId = createTopicConceptCard(auth);

        // Submit review: correct (rating 4) but low confidence (1) = fragile
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 1000L, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stats/fragile")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastRating").value(4))
                .andExpect(jsonPath("$[0].lastConfidence").value(1))
                .andExpect(jsonPath("$[0].occurrences").value(1));
    }

    @Test
    void confidenceIsOptional() throws Exception {
        String auth = registerAndAuth();
        String cardId = createTopicConceptCard(auth);

        // Submit review without confidence — should still work
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 1000L, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confidence").isEmpty());

        // Fragile list should be empty (no confidence data)
        mockMvc.perform(get("/api/stats/fragile")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
