package com.loopy.topic.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.config.ReorderRequest;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.TopicResponse;
import com.loopy.topic.dto.UpdateTopicRequest;
import com.loopy.topic.service.TopicService;
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
 * Controller unit tests for TopicController using @WebMvcTest.
 */
@WebMvcTest(TopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class TopicControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TopicService topicService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    // --- List ---

    @Test
    void list_returns200WithTopics() throws Exception {
        TopicResponse topic = new TopicResponse(UUID.randomUUID(), "Java", "desc", "#FF0000", 1, Instant.now(), Instant.now(), 5);
        when(topicService.getTopics(any(User.class))).thenReturn(List.of(topic));

        mockMvc.perform(get("/api/topics").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[0].cardCount").value(5));
    }

    // --- Get ---

    @Test
    void get_existingTopic_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        TopicResponse topic = new TopicResponse(topicId, "Java", null, "#6366F1", 1, Instant.now(), Instant.now(), 0);
        when(topicService.getTopic(eq(topicId), any(User.class))).thenReturn(topic);

        mockMvc.perform(get("/api/topics/" + topicId).with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void get_nonExistentTopic_returns404() throws Exception {
        UUID topicId = UUID.randomUUID();
        when(topicService.getTopic(eq(topicId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Topic not found"));

        mockMvc.perform(get("/api/topics/" + topicId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Topic not found"));
    }

    // --- Create ---

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateTopicRequest request = new CreateTopicRequest("Java", "Programming", "#FF5733");
        TopicResponse response = new TopicResponse(UUID.randomUUID(), "Java", "Programming", "#FF5733", 1, Instant.now(), Instant.now(), 0);

        when(topicService.createTopic(any(CreateTopicRequest.class), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/topics")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/topics")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Update ---

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        UpdateTopicRequest request = new UpdateTopicRequest("Updated", "New desc", "#00FF00");
        TopicResponse response = new TopicResponse(topicId, "Updated", "New desc", "#00FF00", 1, Instant.now(), Instant.now(), 0);

        when(topicService.updateTopic(eq(topicId), any(UpdateTopicRequest.class), any(User.class))).thenReturn(response);

        mockMvc.perform(put("/api/topics/" + topicId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        UUID topicId = UUID.randomUUID();
        UpdateTopicRequest request = new UpdateTopicRequest("Updated", null, null);

        when(topicService.updateTopic(eq(topicId), any(UpdateTopicRequest.class), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Topic not found"));

        mockMvc.perform(put("/api/topics/" + topicId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Topic not found"));
    }

    // --- Delete ---

    @Test
    void delete_existingTopic_returns204() throws Exception {
        UUID topicId = UUID.randomUUID();

        mockMvc.perform(delete("/api/topics/" + topicId).with(withUser(mockUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        UUID topicId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Topic not found"))
                .when(topicService).deleteTopic(eq(topicId), any(User.class));

        mockMvc.perform(delete("/api/topics/" + topicId).with(withUser(mockUser)))
                .andExpect(status().isNotFound());
    }

    // --- Reorder ---

    @Test
    void reorder_validRequest_returns200() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ReorderRequest request = new ReorderRequest(List.of(id1, id2));

        TopicResponse t1 = new TopicResponse(id1, "A", null, "#6366F1", 1, Instant.now(), Instant.now(), 0);
        TopicResponse t2 = new TopicResponse(id2, "B", null, "#6366F1", 2, Instant.now(), Instant.now(), 0);

        when(topicService.reorderTopics(any(ReorderRequest.class), any(User.class))).thenReturn(List.of(t1, t2));

        mockMvc.perform(put("/api/topics/reorder")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"));
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
