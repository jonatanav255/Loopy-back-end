package com.loopy.card.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.card.service.ConceptService;
import com.loopy.config.ReorderRequest;
import com.loopy.config.ResourceNotFoundException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for ConceptController using @WebMvcTest.
 */
@WebMvcTest(ConceptController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class ConceptControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConceptService conceptService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    private ConceptResponse sampleConcept() {
        return new ConceptResponse(
                UUID.randomUUID(), UUID.randomUUID(), "OOP", "notes",
                null, "LEARNING", 1, Instant.now(), Instant.now()
        );
    }

    // --- List ---

    @Test
    void list_withTopicId_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(conceptService.getConcepts(eq(topicId), any(User.class))).thenReturn(List.of(sampleConcept()));

        mockMvc.perform(get("/api/concepts")
                        .param("topicId", topicId.toString())
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("OOP"));
    }

    // --- Get ---

    @Test
    void get_existingConcept_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        when(conceptService.getConcept(eq(conceptId), any(User.class))).thenReturn(sampleConcept());

        mockMvc.perform(get("/api/concepts/" + conceptId).with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("OOP"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        UUID conceptId = UUID.randomUUID();
        when(conceptService.getConcept(eq(conceptId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Concept not found"));

        mockMvc.perform(get("/api/concepts/" + conceptId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Concept not found"));
    }

    // --- Create ---

    @Test
    void create_validRequest_returns201() throws Exception {
        UUID topicId = UUID.randomUUID();
        CreateConceptRequest request = new CreateConceptRequest(topicId, "OOP Basics", "Some notes");

        when(conceptService.createConcept(any(CreateConceptRequest.class), any(User.class)))
                .thenReturn(sampleConcept());

        mockMvc.perform(post("/api/concepts")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        UUID topicId = UUID.randomUUID();
        mockMvc.perform(post("/api/concepts")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topicId\":\"" + topicId + "\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void create_nullTopicId_returns400() throws Exception {
        mockMvc.perform(post("/api/concepts")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"OOP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Update ---

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        UpdateConceptRequest request = new UpdateConceptRequest("Updated Title", "Updated notes", null);

        when(conceptService.updateConcept(eq(conceptId), any(UpdateConceptRequest.class), any(User.class)))
                .thenReturn(sampleConcept());

        mockMvc.perform(put("/api/concepts/" + conceptId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // --- Delete ---

    @Test
    void delete_existingConcept_returns204() throws Exception {
        UUID conceptId = UUID.randomUUID();

        mockMvc.perform(delete("/api/concepts/" + conceptId).with(withUser(mockUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        UUID conceptId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Concept not found"))
                .when(conceptService).deleteConcept(eq(conceptId), any(User.class));

        mockMvc.perform(delete("/api/concepts/" + conceptId).with(withUser(mockUser)))
                .andExpect(status().isNotFound());
    }

    // --- Reorder ---

    @Test
    void reorder_validRequest_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        ReorderRequest request = new ReorderRequest(List.of(id1));

        when(conceptService.reorderConcepts(eq(topicId), any(ReorderRequest.class), any(User.class)))
                .thenReturn(List.of(sampleConcept()));

        mockMvc.perform(put("/api/concepts/reorder")
                        .param("topicId", topicId.toString())
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
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
