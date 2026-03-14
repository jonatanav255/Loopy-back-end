package com.loopy.review.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.CardResponse;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.review.dto.ReviewResponse;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.review.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for ReviewController using @WebMvcTest.
 */
@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class ReviewControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    private CardResponse sampleCard() {
        return new CardResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Q", "A", "STANDARD",
                null, null, 0, 2.5, 0, LocalDate.now(), null,
                0, 0, "SM2", Instant.now(), Instant.now()
        );
    }

    // --- Today ---

    @Test
    void today_returns200() throws Exception {
        when(reviewService.getDueCards(any(User.class), any(), any()))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/reviews/today").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("Q"));
    }

    @Test
    void today_withLimitAndTopicIds_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(reviewService.getDueCards(any(User.class), any(), any()))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/reviews/today")
                        .param("limit", "10")
                        .param("topicIds", topicId.toString())
                        .with(withUser(mockUser)))
                .andExpect(status().isOk());
    }

    // --- Practice ---

    @Test
    void practice_returns200() throws Exception {
        when(reviewService.getPracticeCards(any(User.class), any()))
                .thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/reviews/practice").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("Q"));
    }

    // --- Submit ---

    @Test
    void submit_validRequest_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        SubmitReviewRequest request = new SubmitReviewRequest(4, 1500L, 2);
        ReviewResponse response = new ReviewResponse(
                UUID.randomUUID(), 4, 1500L, 2, Instant.now(), sampleCard()
        );

        when(reviewService.submitReview(eq(cardId), any(SubmitReviewRequest.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.updatedCard").exists());
    }

    @Test
    void submit_nullRating_returns400() throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responseTimeMs\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_ratingTooHigh_returns400() throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_ratingTooLow_returns400() throws Exception {
        UUID cardId = UUID.randomUUID();
        mockMvc.perform(post("/api/reviews/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_notFound_returns404() throws Exception {
        UUID cardId = UUID.randomUUID();
        SubmitReviewRequest request = new SubmitReviewRequest(3, null, null);

        when(reviewService.submitReview(eq(cardId), any(SubmitReviewRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(post("/api/reviews/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    private static void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
