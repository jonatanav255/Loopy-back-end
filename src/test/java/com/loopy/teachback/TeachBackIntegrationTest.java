package com.loopy.teachback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.topic.dto.CreateTopicRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeachBackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String auth;
    private UUID topicId;
    private UUID conceptId;
    private String cardId;

    @BeforeEach
    void setUp() throws Exception {
        // Register user
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("tb-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        auth = "Bearer " + objectMapper.readValue(
                authResult.getResponse().getContentAsString(), TokenResponse.class).accessToken();

        // Create topic
        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("TB Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        topicId = UUID.fromString(objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText());

        // Create concept
        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, "TB Concept", "notes"))))
                .andExpect(status().isCreated())
                .andReturn();
        conceptId = UUID.fromString(objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText());

        // Create card
        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, "Q?", "A.", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        cardId = objectMapper.readTree(
                cardResult.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void escalationTriggersAfterThreeFailures() throws Exception {
        // Submit 3 failing reviews (rating < 3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/reviews/" + cardId)
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SubmitReviewRequest(1, 500L, null))))
                    .andExpect(status().isOk());
        }

        // Concept should now be TEACH_BACK_REQUIRED
        mockMvc.perform(get("/api/concepts/" + conceptId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEACH_BACK_REQUIRED"));

        // Should appear in pending teach-backs
        mockMvc.perform(get("/api/teach-back/pending")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conceptId.toString()));
    }

    @Test
    void submitTeachBack_andHistory() throws Exception {
        // Set reference explanation on concept
        mockMvc.perform(put("/api/concepts/" + conceptId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateConceptRequest("TB Concept", "notes",
                                        "This is the reference explanation."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceExplanation").value("This is the reference explanation."));

        // Submit teach-back
        mockMvc.perform(post("/api/teach-back")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitTeachBackRequest(conceptId,
                                        "My explanation of the concept...",
                                        4,
                                        List.of("Missed edge case", "Forgot syntax")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conceptTitle").value("TB Concept"))
                .andExpect(jsonPath("$.userExplanation").value("My explanation of the concept..."))
                .andExpect(jsonPath("$.referenceExplanation").value("This is the reference explanation."))
                .andExpect(jsonPath("$.selfRating").value(4))
                .andExpect(jsonPath("$.gapsFound[0]").value("Missed edge case"))
                .andExpect(jsonPath("$.gapsFound[1]").value("Forgot syntax"));

        // Check history
        mockMvc.perform(get("/api/teach-back/history")
                        .param("conceptId", conceptId.toString())
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].selfRating").value(4));
    }

    @Test
    void highSelfRating_movesConceptToReview() throws Exception {
        // Force concept to TEACH_BACK_REQUIRED via 3 failures
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/reviews/" + cardId)
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SubmitReviewRequest(1, 500L, null))))
                    .andExpect(status().isOk());
        }

        // Verify escalated
        mockMvc.perform(get("/api/concepts/" + conceptId)
                        .header("Authorization", auth))
                .andExpect(jsonPath("$.status").value("TEACH_BACK_REQUIRED"));

        // Submit high-rated teach-back (selfRating >= 4 → moves to REVIEW)
        mockMvc.perform(post("/api/teach-back")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitTeachBackRequest(conceptId,
                                        "Thorough explanation here.",
                                        4, null))))
                .andExpect(status().isCreated());

        // Concept should now be REVIEW
        mockMvc.perform(get("/api/concepts/" + conceptId)
                        .header("Authorization", auth))
                .andExpect(jsonPath("$.status").value("REVIEW"));
    }
}
