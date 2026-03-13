package com.loopy.topic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.UpdateTopicRequest;
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
class TopicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class).accessToken();
    }

    @Test
    void crudFlow() throws Exception {
        String auth = "Bearer " + registerAndGetToken("topic-crud@example.com");

        // Create
        MvcResult createResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("Spring Boot", "Backend framework", "#FF5733"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring Boot"))
                .andExpect(jsonPath("$.colorHex").value("#FF5733"))
                .andReturn();

        String topicId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText();

        // Read
        mockMvc.perform(get("/api/topics/" + topicId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring Boot"));

        // List
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Spring Boot"));

        // Update
        mockMvc.perform(put("/api/topics/" + topicId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTopicRequest("Spring Framework", "Full framework", "#00FF00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spring Framework"));

        // Delete
        mockMvc.perform(delete("/api/topics/" + topicId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/topics/" + topicId)
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipIsolation() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("topic-user1@example.com");
        String auth2 = "Bearer " + registerAndGetToken("topic-user2@example.com");

        // User 1 creates a topic
        MvcResult createResult = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("User1 Topic", null, null))))
                .andExpect(status().isCreated())
                .andReturn();

        String topicId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText();

        // User 2 cannot see it
        mockMvc.perform(get("/api/topics/" + topicId)
                        .header("Authorization", auth2))
                .andExpect(status().isNotFound());

        // User 2's list is empty
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
