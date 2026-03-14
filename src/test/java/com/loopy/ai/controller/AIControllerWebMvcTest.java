package com.loopy.ai.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.ai.dto.EvaluateTeachBackRequest;
import com.loopy.ai.dto.GenerateCardsRequest;
import com.loopy.ai.dto.GeneratedCard;
import com.loopy.ai.dto.TeachBackEvaluation;
import com.loopy.ai.service.AIService;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.config.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for AIController using @WebMvcTest.
 */
@WebMvcTest(AIController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class AIControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIService aiService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    // --- Status ---

    @Test
    void status_available_returns200() throws Exception {
        when(aiService.isAvailable()).thenReturn(true);

        mockMvc.perform(get("/api/ai/status").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void status_unavailable_returns200() throws Exception {
        when(aiService.isAvailable()).thenReturn(false);

        mockMvc.perform(get("/api/ai/status").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    // --- Generate Cards ---

    @Test
    void generateCards_validRequest_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        GenerateCardsRequest request = new GenerateCardsRequest(conceptId, "Java OOP content", 3);
        GeneratedCard card = new GeneratedCard("What is OOP?", "Object-Oriented Programming", "STANDARD", null);

        when(aiService.generateCards(eq(conceptId), eq("Java OOP content"), eq(3), any(User.class)))
                .thenReturn(List.of(card));

        mockMvc.perform(post("/api/ai/generate-cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("What is OOP?"));
    }

    @Test
    void generateCards_blankContent_returns400() throws Exception {
        UUID conceptId = UUID.randomUUID();
        mockMvc.perform(post("/api/ai/generate-cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptId\":\"" + conceptId + "\",\"content\":\"\",\"numCards\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void generateCards_conceptNotFound_returns404() throws Exception {
        UUID conceptId = UUID.randomUUID();
        GenerateCardsRequest request = new GenerateCardsRequest(conceptId, "content", 3);

        when(aiService.generateCards(eq(conceptId), anyString(), anyInt(), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Concept not found"));

        mockMvc.perform(post("/api/ai/generate-cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- Evaluate Teach-Back ---

    @Test
    void evaluateTeachBack_validRequest_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        EvaluateTeachBackRequest request = new EvaluateTeachBackRequest(conceptId, "My explanation...");
        TeachBackEvaluation evaluation = new TeachBackEvaluation(
                8, 7, 6, "Good explanation", List.of("What about X?"),
                List.of("Missing Y"), List.of()
        );

        when(aiService.evaluateTeachBack(eq(conceptId), eq("My explanation..."), any(User.class)))
                .thenReturn(evaluation);

        mockMvc.perform(post("/api/ai/evaluate-teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clarityScore").value(8))
                .andExpect(jsonPath("$.feedback").value("Good explanation"));
    }

    @Test
    void evaluateTeachBack_blankExplanation_returns400() throws Exception {
        UUID conceptId = UUID.randomUUID();
        mockMvc.perform(post("/api/ai/evaluate-teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptId\":\"" + conceptId + "\",\"userExplanation\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
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
