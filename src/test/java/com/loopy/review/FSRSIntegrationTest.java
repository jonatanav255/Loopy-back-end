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
class FSRSIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndAuth() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("fsrs-" + UUID.randomUUID() + "@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return "Bearer " + objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    private String createCard(String auth) throws Exception {
        MvcResult topicResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("FSRS Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID topicId = UUID.fromString(objectMapper.readTree(
                topicResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult conceptResult = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(topicId, "FSRS Concept", "notes"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID conceptId = UUID.fromString(objectMapper.readTree(
                conceptResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult cardResult = mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(conceptId, "FSRS Q?", "FSRS A.", null, null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(
                cardResult.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void newCardDefaults_toSM2() throws Exception {
        String auth = registerAndAuth();
        String cardId = createCard(auth);

        mockMvc.perform(get("/api/cards/" + cardId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedulingAlgorithm").value("SM2"))
                .andExpect(jsonPath("$.stability").value(0.0))
                .andExpect(jsonPath("$.difficulty").value(0.0));
    }

    @Test
    void switchToFSRS_andReview() throws Exception {
        String auth = registerAndAuth();
        String cardId = createCard(auth);

        // Switch to FSRS
        mockMvc.perform(put("/api/cards/" + cardId + "/algorithm")
                        .param("algorithm", "FSRS")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedulingAlgorithm").value("FSRS"));

        // Submit review — should use FSRS
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 1000L, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCard.schedulingAlgorithm").value("FSRS"))
                .andExpect(jsonPath("$.updatedCard.stability").isNumber())
                .andExpect(jsonPath("$.updatedCard.difficulty").isNumber())
                .andExpect(jsonPath("$.updatedCard.repetitionCount").value(1));

        // Verify card state persisted
        mockMvc.perform(get("/api/cards/" + cardId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stability").isNumber())
                .andExpect(jsonPath("$.difficulty").isNumber())
                .andExpect(jsonPath("$.schedulingAlgorithm").value("FSRS"));
    }

    @Test
    void sm2Card_reviewStillWorks() throws Exception {
        String auth = registerAndAuth();
        String cardId = createCard(auth);

        // Review with SM2 (default)
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitReviewRequest(4, 800L, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCard.schedulingAlgorithm").value("SM2"))
                .andExpect(jsonPath("$.updatedCard.repetitionCount").value(1))
                .andExpect(jsonPath("$.updatedCard.intervalDays").value(1));
    }
}
