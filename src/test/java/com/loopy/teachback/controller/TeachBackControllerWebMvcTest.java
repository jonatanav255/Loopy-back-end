package com.loopy.teachback.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.teachback.dto.TeachBackResponse;
import com.loopy.teachback.service.TeachBackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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
 * Controller unit tests for TeachBackController using @WebMvcTest.
 */
@WebMvcTest(TeachBackController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class TeachBackControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeachBackService teachBackService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    // --- Pending ---

    @Test
    void pending_returns200() throws Exception {
        ConceptResponse concept = new ConceptResponse(
                UUID.randomUUID(), UUID.randomUUID(), "OOP", null, null,
                "TEACH_BACK_REQUIRED", 1, Instant.now(), Instant.now()
        );
        when(teachBackService.getConceptsRequiringTeachBack(any(User.class)))
                .thenReturn(List.of(concept));

        mockMvc.perform(get("/api/teach-back/pending").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("OOP"))
                .andExpect(jsonPath("$[0].status").value("TEACH_BACK_REQUIRED"));
    }

    // --- Submit ---

    @Test
    void submit_validRequest_returns201() throws Exception {
        UUID conceptId = UUID.randomUUID();
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "My explanation of OOP...", 4, List.of("Polymorphism")
        );
        TeachBackResponse response = new TeachBackResponse(
                UUID.randomUUID(), conceptId, "OOP",
                "My explanation of OOP...", null, 4, List.of("Polymorphism"),
                Instant.now()
        );

        when(teachBackService.submitTeachBack(any(SubmitTeachBackRequest.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.selfRating").value(4))
                .andExpect(jsonPath("$.userExplanation").value("My explanation of OOP..."));
    }

    @Test
    void submit_nullConceptId_returns400() throws Exception {
        mockMvc.perform(post("/api/teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userExplanation\":\"test\",\"selfRating\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_blankExplanation_returns400() throws Exception {
        UUID conceptId = UUID.randomUUID();
        mockMvc.perform(post("/api/teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptId\":\"" + conceptId + "\",\"userExplanation\":\"\",\"selfRating\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_ratingOutOfRange_returns400() throws Exception {
        UUID conceptId = UUID.randomUUID();
        mockMvc.perform(post("/api/teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptId\":\"" + conceptId + "\",\"userExplanation\":\"test\",\"selfRating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submit_notFound_returns404() throws Exception {
        UUID conceptId = UUID.randomUUID();
        SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                conceptId, "My explanation", 3, null
        );

        when(teachBackService.submitTeachBack(any(SubmitTeachBackRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Concept not found"));

        mockMvc.perform(post("/api/teach-back")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- History ---

    @Test
    void history_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        TeachBackResponse tb = new TeachBackResponse(
                UUID.randomUUID(), conceptId, "OOP",
                "explanation", null, 4, List.of(), Instant.now()
        );
        when(teachBackService.getHistory(eq(conceptId), any(User.class)))
                .thenReturn(List.of(tb));

        mockMvc.perform(get("/api/teach-back/history")
                        .param("conceptId", conceptId.toString())
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].selfRating").value(4));
    }

    @Test
    void history_notFound_returns404() throws Exception {
        UUID conceptId = UUID.randomUUID();
        when(teachBackService.getHistory(eq(conceptId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Concept not found"));

        mockMvc.perform(get("/api/teach-back/history")
                        .param("conceptId", conceptId.toString())
                        .with(withUser(mockUser)))
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
