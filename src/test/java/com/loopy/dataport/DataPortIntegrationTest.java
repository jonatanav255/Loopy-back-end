package com.loopy.dataport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.entity.CardType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DataPortIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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

    private String createTopic(String auth, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest(name, "Description", "#FF0000"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createConcept(String auth, String topicId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(java.util.UUID.fromString(topicId), title, "Notes"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createCard(String auth, String conceptId, String front, String back) throws Exception {
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(java.util.UUID.fromString(conceptId), front, back,
                                        CardType.STANDARD, null, null))))
                .andExpect(status().isCreated());
    }

    @Test
    void exportAll_returnsNestedJsonWithMetadata() throws Exception {
        String auth = "Bearer " + registerAndGetToken("export-all@example.com");

        String topicId = createTopic(auth, "Java Basics");
        String conceptId = createConcept(auth, topicId, "OOP");
        createCard(auth, conceptId, "What is OOP?", "Object-Oriented Programming");
        createCard(auth, conceptId, "What is inheritance?", "A mechanism for code reuse");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportVersion").value("1.0"))
                .andExpect(jsonPath("$.exportedAt").exists())
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.conceptCount").value(1))
                .andExpect(jsonPath("$.cardCount").value(2))
                .andExpect(jsonPath("$.topics[0].name").value("Java Basics"))
                .andExpect(jsonPath("$.topics[0].concepts[0].title").value("OOP"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards", hasSize(2)));
    }

    @Test
    void exportSpecificTopics_filtersCorrectly() throws Exception {
        String auth = "Bearer " + registerAndGetToken("export-filter@example.com");

        String topic1 = createTopic(auth, "Topic A");
        String topic2 = createTopic(auth, "Topic B");
        createConcept(auth, topic1, "Concept A");
        createConcept(auth, topic2, "Concept B");

        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", topic1)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.topics[0].name").value("Topic A"));
    }

    @Test
    void importData_createsEntitiesForImporter() throws Exception {
        String auth = "Bearer " + registerAndGetToken("importer@example.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [
                        {
                            "name": "Imported Topic",
                            "description": "From another account",
                            "colorHex": "#00FF00",
                            "concepts": [
                                {
                                    "title": "Imported Concept",
                                    "notes": "Some notes",
                                    "referenceExplanation": "An explanation",
                                    "cards": [
                                        {
                                            "front": "Q1",
                                            "back": "A1",
                                            "cardType": "STANDARD",
                                            "hint": "A hint"
                                        },
                                        {
                                            "front": "Q2",
                                            "back": "A2",
                                            "cardType": "CODE_OUTPUT"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(2));

        // Verify the imported data shows up in the user's topics
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Imported Topic"))
                .andExpect(jsonPath("$[0].colorHex").value("#00FF00"));
    }

    @Test
    void roundTrip_exportThenImportOnDifferentAccount() throws Exception {
        // User 1 creates data
        String auth1 = "Bearer " + registerAndGetToken("roundtrip-user1@example.com");
        String topicId = createTopic(auth1, "Shared Knowledge");
        String conceptId = createConcept(auth1, topicId, "Key Concept");
        createCard(auth1, conceptId, "What is X?", "X is something important");

        // User 1 exports
        MvcResult exportResult = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andReturn();

        String exportJson = exportResult.getResponse().getContentAsString();

        // User 2 imports the same data
        String auth2 = "Bearer " + registerAndGetToken("roundtrip-user2@example.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(1));

        // User 2 can see the imported topic
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Shared Knowledge"));

        // User 1's data is untouched
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void importData_validation_rejectsEmptyTopics() throws Exception {
        String auth = "Bearer " + registerAndGetToken("import-invalid@example.com");

        String invalidJson = """
                {
                    "exportVersion": "1.0",
                    "topics": []
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importData_validation_rejectsMissingVersion() throws Exception {
        String auth = "Bearer " + registerAndGetToken("import-noversion@example.com");

        String invalidJson = """
                {
                    "topics": [{"name": "Topic", "concepts": []}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/dataport/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void importRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\",\"topics\":[]}"))
                .andExpect(status().isForbidden());
    }
}
