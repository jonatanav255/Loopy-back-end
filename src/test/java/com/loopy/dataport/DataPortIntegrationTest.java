package com.loopy.dataport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.entity.CardType;
import com.loopy.topic.dto.CreateTopicRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DataPortIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── Helpers ──

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

    private String createTopic(String auth, String name, String desc, String color) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/topics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest(name, desc, color))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTopic(String auth, String name) throws Exception {
        return createTopic(auth, name, "Description", "#FF0000");
    }

    private String createConcept(String auth, String topicId, String title, String notes) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/concepts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateConceptRequest(UUID.fromString(topicId), title, notes))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createConcept(String auth, String topicId, String title) throws Exception {
        return createConcept(auth, topicId, title, "Notes");
    }

    private void createCard(String auth, String conceptId, String front, String back,
                            CardType type, String hint, String sourceUrl) throws Exception {
        mockMvc.perform(post("/api/cards")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCardRequest(UUID.fromString(conceptId), front, back,
                                        type, hint, sourceUrl))))
                .andExpect(status().isCreated());
    }

    private void createCard(String auth, String conceptId, String front, String back) throws Exception {
        createCard(auth, conceptId, front, back, CardType.STANDARD, null, null);
    }

    // ═══════════════════════════════════════════════
    // EXPORT TESTS
    // ═══════════════════════════════════════════════

    @Test
    void export_returnsNestedJsonWithMetadata() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-meta@test.com");

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
                .andExpect(jsonPath("$.topics[0].description").value("Description"))
                .andExpect(jsonPath("$.topics[0].colorHex").value("#FF0000"))
                .andExpect(jsonPath("$.topics[0].concepts[0].title").value("OOP"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards", hasSize(2)));
    }

    @Test
    void export_emptyAccount_returnsZeroCounts() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-empty@test.com");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(0))
                .andExpect(jsonPath("$.conceptCount").value(0))
                .andExpect(jsonPath("$.cardCount").value(0))
                .andExpect(jsonPath("$.topics").isEmpty());
    }

    @Test
    void export_preservesAllCardFields() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-fields@test.com");

        String topicId = createTopic(auth, "Fields Test");
        String conceptId = createConcept(auth, topicId, "Field Concept");
        createCard(auth, conceptId, "Front text", "Back text",
                CardType.CODE_OUTPUT, "A helpful hint", "https://example.com/source");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].front").value("Front text"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].back").value("Back text"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].cardType").value("CODE_OUTPUT"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].hint").value("A helpful hint"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].sourceUrl").value("https://example.com/source"));
    }

    @Test
    void export_multipleCardTypes_allExported() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-types@test.com");

        String topicId = createTopic(auth, "Card Types");
        String conceptId = createConcept(auth, topicId, "Types Concept");
        createCard(auth, conceptId, "Q1", "A1", CardType.STANDARD, null, null);
        createCard(auth, conceptId, "Q2", "A2", CardType.CODE_OUTPUT, null, null);
        createCard(auth, conceptId, "Q3", "A3", CardType.SPOT_THE_BUG, null, null);
        createCard(auth, conceptId, "Q4", "A4", CardType.FILL_BLANK, null, null);
        createCard(auth, conceptId, "Q5", "A5", CardType.EXPLAIN_WHEN, null, null);
        createCard(auth, conceptId, "Q6", "A6", CardType.COMPARE, null, null);

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardCount").value(6))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards", hasSize(6)));
    }

    @Test
    void export_topicWithConceptsButNoCards() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-nocards@test.com");

        String topicId = createTopic(auth, "No Cards Topic");
        createConcept(auth, topicId, "Empty Concept");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.conceptCount").value(1))
                .andExpect(jsonPath("$.cardCount").value(0))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards").isEmpty());
    }

    @Test
    void export_topicWithNoConcepts() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-noconcepts@test.com");

        createTopic(auth, "No Concepts Topic");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.conceptCount").value(0))
                .andExpect(jsonPath("$.topics[0].concepts").isEmpty());
    }

    @Test
    void export_multipleTopicsWithMixedData() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-mixed@test.com");

        String t1 = createTopic(auth, "Topic A");
        String t2 = createTopic(auth, "Topic B");
        String c1 = createConcept(auth, t1, "Concept A1");
        createConcept(auth, t1, "Concept A2");
        String c3 = createConcept(auth, t2, "Concept B1");
        createCard(auth, c1, "Q1", "A1");
        createCard(auth, c3, "Q2", "A2");
        createCard(auth, c3, "Q3", "A3");

        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(2))
                .andExpect(jsonPath("$.conceptCount").value(3))
                .andExpect(jsonPath("$.cardCount").value(3));
    }

    // ── Export: filtered by topic IDs ──

    @Test
    void export_specificTopic_filtersCorrectly() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-specific@test.com");

        String topic1 = createTopic(auth, "Selected Topic");
        String topic2 = createTopic(auth, "Not Selected");
        createConcept(auth, topic1, "Selected Concept");
        createConcept(auth, topic2, "Not Selected Concept");

        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", topic1)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(1))
                .andExpect(jsonPath("$.topics[0].name").value("Selected Topic"));
    }

    @Test
    void export_multipleSpecificTopics() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-multi@test.com");

        String t1 = createTopic(auth, "First");
        String t2 = createTopic(auth, "Second");
        createTopic(auth, "Third");

        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", t1, t2)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(2))
                .andExpect(jsonPath("$.topics", hasSize(2)));
    }

    @Test
    void export_nonexistentTopicId_returns404() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-404@test.com");

        UUID fakeId = UUID.randomUUID();
        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", fakeId.toString())
                        .header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void export_otherUsersTopicId_returns404() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("dp-export-owner1@test.com");
        String auth2 = "Bearer " + registerAndGetToken("dp-export-owner2@test.com");

        String user1Topic = createTopic(auth1, "User1 Topic");

        // User 2 tries to export User 1's topic
        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", user1Topic)
                        .header("Authorization", auth2))
                .andExpect(status().isNotFound());
    }

    @Test
    void export_noIdsInExportedJson() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-noids@test.com");

        String topicId = createTopic(auth, "Test");
        String conceptId = createConcept(auth, topicId, "Concept");
        createCard(auth, conceptId, "Q", "A");

        MvcResult result = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode topic = json.get("topics").get(0);
        assertFalse(topic.has("id"), "Topic should not have id in export");
        JsonNode concept = topic.get("concepts").get(0);
        assertFalse(concept.has("id"), "Concept should not have id in export");
        JsonNode card = concept.get("cards").get(0);
        assertFalse(card.has("id"), "Card should not have id in export");
    }

    @Test
    void export_noSchedulingStateInExportedJson() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-export-nosched@test.com");

        String topicId = createTopic(auth, "Test");
        String conceptId = createConcept(auth, topicId, "Concept");
        createCard(auth, conceptId, "Q", "A");

        MvcResult result = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode card = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("topics").get(0).get("concepts").get(0).get("cards").get(0);
        assertFalse(card.has("repetitionCount"), "Card should not have scheduling state");
        assertFalse(card.has("easeFactor"), "Card should not have scheduling state");
        assertFalse(card.has("intervalDays"), "Card should not have scheduling state");
        assertFalse(card.has("nextReviewDate"), "Card should not have scheduling state");
    }

    // ═══════════════════════════════════════════════
    // IMPORT TESTS
    // ═══════════════════════════════════════════════

    @Test
    void import_createsEntitiesForImporter() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-basic@test.com");

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
    void import_topicOnlyNoConcepts() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-topiconly@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [
                        {"name": "Bare Topic", "description": "No concepts"}
                    ]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(0))
                .andExpect(jsonPath("$.cardsCreated").value(0));
    }

    @Test
    void import_conceptWithEmptyCards() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-emptycards@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{"title": "C", "cards": []}]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(0));
    }

    @Test
    void import_differentCardTypes() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-types@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [
                                {"front": "Q1", "back": "A1", "cardType": "STANDARD"},
                                {"front": "Q2", "back": "A2", "cardType": "CODE_OUTPUT"},
                                {"front": "Q3", "back": "A3", "cardType": "SPOT_THE_BUG"},
                                {"front": "Q4", "back": "A4", "cardType": "FILL_BLANK"},
                                {"front": "Q5", "back": "A5", "cardType": "EXPLAIN_WHEN"},
                                {"front": "Q6", "back": "A6", "cardType": "COMPARE"}
                            ]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardsCreated").value(6));
    }

    @Test
    void import_unknownCardType_fallsBackToStandard() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-badtype@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "Q", "back": "A", "cardType": "NONEXISTENT"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardsCreated").value(1));
    }

    @Test
    void import_nullCardType_defaultsToStandard() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-nulltype@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "Q", "back": "A"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardsCreated").value(1));
    }

    @Test
    void import_defaultColor_whenNotProvided() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-defcolor@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{"name": "No Color"}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].colorHex").value("#6366F1"));
    }

    @Test
    void import_multipleTopics() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-multitopic@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [
                        {"name": "Topic A", "colorHex": "#AA0000"},
                        {"name": "Topic B", "colorHex": "#BB0000"},
                        {"name": "Topic C", "colorHex": "#CC0000"}
                    ]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(3));

        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void import_sameTwice_createsDuplicates() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-dupe@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{"name": "Duped Topic"}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void import_extraFieldsIgnored() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-extra@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "exportedAt": "2025-01-01T00:00:00Z",
                    "topicCount": 999,
                    "unknownField": "ignored",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "Q", "back": "A", "extraField": "ignore me"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(1));
    }

    @Test
    void import_cardsHaveFreshSchedulingState() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-sched@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "Q", "back": "A"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated());

        // Fetch the imported cards to verify scheduling defaults
        MvcResult topicsResult = mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();

        String topicId = objectMapper.readTree(
                topicsResult.getResponse().getContentAsString()).get(0).get("id").asText();

        MvcResult conceptsResult = mockMvc.perform(get("/api/concepts")
                        .param("topicId", topicId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();

        String conceptId = objectMapper.readTree(
                conceptsResult.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(get("/api/cards")
                        .param("conceptId", conceptId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].repetitionCount").value(0))
                .andExpect(jsonPath("$[0].easeFactor").value(2.5))
                .andExpect(jsonPath("$[0].intervalDays").value(0))
                .andExpect(jsonPath("$[0].schedulingAlgorithm").value("SM2"));
    }

    // ═══════════════════════════════════════════════
    // VALIDATION TESTS
    // ═══════════════════════════════════════════════

    @Test
    void import_rejectsEmptyTopics() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val1@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\",\"topics\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsMissingVersion() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val2@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topics\":[{\"name\":\"T\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsNullTopics() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val3@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsBlankTopicName() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val4@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{"name": "   "}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsMissingTopicName() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val5@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{"description": "no name"}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsBlankConceptTitle() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val6@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{"title": ""}]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsBlankCardFront() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val7@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "", "back": "A"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsBlankCardBack() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val8@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"front": "Q", "back": "   "}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsMissingCardFields() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val9@test.com");

        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{
                        "name": "T",
                        "concepts": [{
                            "title": "C",
                            "cards": [{"hint": "only a hint"}]
                        }]
                    }]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsMalformedJson() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val10@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void import_rejectsEmptyBody() throws Exception {
        String auth = "Bearer " + registerAndGetToken("dp-import-val11@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════
    // ROUND-TRIP TESTS
    // ═══════════════════════════════════════════════

    @Test
    void roundTrip_exportThenImportOnDifferentAccount() throws Exception {
        // User 1 creates data
        String auth1 = "Bearer " + registerAndGetToken("dp-rt-user1@test.com");
        String topicId = createTopic(auth1, "Shared Knowledge", "To be shared", "#AABB00");
        String conceptId = createConcept(auth1, topicId, "Key Concept", "Important notes");
        createCard(auth1, conceptId, "What is X?", "X is something important");

        // User 1 exports
        MvcResult exportResult = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andReturn();

        String exportJson = exportResult.getResponse().getContentAsString();

        // User 2 imports the same data
        String auth2 = "Bearer " + registerAndGetToken("dp-rt-user2@test.com");

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(1));

        // User 2 can see the imported topic with correct data
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Shared Knowledge"))
                .andExpect(jsonPath("$[0].colorHex").value("#AABB00"));

        // User 1's data is untouched
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void roundTrip_preservesAllFieldsAccurately() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("dp-rt-fields1@test.com");

        String topicId = createTopic(auth1, "Topic Name", "Topic Desc", "#FFAA00");
        String conceptId = createConcept(auth1, topicId, "Concept Title", "Concept Notes");
        createCard(auth1, conceptId, "Card Front", "Card Back",
                CardType.SPOT_THE_BUG, "Card Hint", "https://src.io");

        // Export
        MvcResult exportResult = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andReturn();

        // Import on different account
        String auth2 = "Bearer " + registerAndGetToken("dp-rt-fields2@test.com");
        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportResult.getResponse().getContentAsString()))
                .andExpect(status().isCreated());

        // Re-export from user 2 and compare
        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("Topic Name"))
                .andExpect(jsonPath("$.topics[0].description").value("Topic Desc"))
                .andExpect(jsonPath("$.topics[0].colorHex").value("#FFAA00"))
                .andExpect(jsonPath("$.topics[0].concepts[0].title").value("Concept Title"))
                .andExpect(jsonPath("$.topics[0].concepts[0].notes").value("Concept Notes"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].front").value("Card Front"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].back").value("Card Back"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].cardType").value("SPOT_THE_BUG"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].hint").value("Card Hint"))
                .andExpect(jsonPath("$.topics[0].concepts[0].cards[0].sourceUrl").value("https://src.io"));
    }

    @Test
    void roundTrip_multipleTopicsWithMixedNesting() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("dp-rt-complex1@test.com");

        // Topic with concepts and cards
        String t1 = createTopic(auth1, "Full Topic");
        String c1 = createConcept(auth1, t1, "Full Concept");
        createCard(auth1, c1, "Q1", "A1");
        createCard(auth1, c1, "Q2", "A2");

        // Topic with concept but no cards
        String t2 = createTopic(auth1, "Partial Topic");
        createConcept(auth1, t2, "Empty Concept");

        // Topic with no concepts
        createTopic(auth1, "Bare Topic");

        // Export
        MvcResult exportResult = mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(3))
                .andExpect(jsonPath("$.conceptCount").value(2))
                .andExpect(jsonPath("$.cardCount").value(2))
                .andReturn();

        // Import on different account
        String auth2 = "Bearer " + registerAndGetToken("dp-rt-complex2@test.com");
        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exportResult.getResponse().getContentAsString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(3))
                .andExpect(jsonPath("$.conceptsCreated").value(2))
                .andExpect(jsonPath("$.cardsCreated").value(2));
    }

    // ═══════════════════════════════════════════════
    // SECURITY TESTS
    // ═══════════════════════════════════════════════

    @Test
    void export_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/dataport/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    void import_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\",\"topics\":[{\"name\":\"T\"}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void export_withMalformedToken_returns403() throws Exception {
        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void import_withMalformedToken_returns403() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", "Bearer abc.def.ghi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\",\"topics\":[{\"name\":\"T\"}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void export_ownershipIsolation_usersCannotSeeEachOthersData() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("dp-sec-iso1@test.com");
        String auth2 = "Bearer " + registerAndGetToken("dp-sec-iso2@test.com");

        createTopic(auth1, "User1 Secret Topic");

        // User 2's export is empty
        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(0))
                .andExpect(jsonPath("$.topics").isEmpty());

        // User 1 sees their topic
        mockMvc.perform(get("/api/dataport/export")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicCount").value(1));
    }

    @Test
    void import_ownershipIsolation_importedDataBelongsToImporter() throws Exception {
        String auth1 = "Bearer " + registerAndGetToken("dp-sec-own1@test.com");
        String auth2 = "Bearer " + registerAndGetToken("dp-sec-own2@test.com");

        // User 2 imports data
        String importJson = """
                {
                    "exportVersion": "1.0",
                    "topics": [{"name": "Imported By User2"}]
                }
                """;

        mockMvc.perform(post("/api/dataport/import")
                        .header("Authorization", auth2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importJson))
                .andExpect(status().isCreated());

        // User 1 cannot see User 2's imported data
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // User 2 can see it
        mockMvc.perform(get("/api/topics")
                        .header("Authorization", auth2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Imported By User2"));
    }
}
