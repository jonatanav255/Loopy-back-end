package com.loopy.card.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.UpdateCardRequest;
import com.loopy.card.entity.CardType;
import com.loopy.card.service.CardService;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.review.service.SchedulingAlgorithm;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for CardController using @WebMvcTest.
 */
@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class CardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

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
                UUID.randomUUID(), UUID.randomUUID(), "Front", "Back", "STANDARD",
                "hint", null, 0, 2.5, 0, LocalDate.now(), null,
                0, 0, "SM2", Instant.now(), Instant.now()
        );
    }

    // --- List ---

    @Test
    void list_withConceptId_returns200() throws Exception {
        UUID conceptId = UUID.randomUUID();
        when(cardService.getCards(eq(conceptId), any(User.class))).thenReturn(List.of(sampleCard()));

        mockMvc.perform(get("/api/cards")
                        .param("conceptId", conceptId.toString())
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("Front"));
    }

    @Test
    void list_missingConceptId_returns400() throws Exception {
        mockMvc.perform(get("/api/cards").with(withUser(mockUser)))
                .andExpect(status().isBadRequest());
    }

    // --- Get ---

    @Test
    void get_existingCard_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class))).thenReturn(sampleCard());

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.front").value("Front"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Card not found"));
    }

    // --- Create ---

    @Test
    void create_validRequest_returns201() throws Exception {
        UUID conceptId = UUID.randomUUID();
        CreateCardRequest request = new CreateCardRequest(conceptId, "Q?", "A!", CardType.STANDARD, null, null);

        when(cardService.createCard(any(CreateCardRequest.class), any(User.class))).thenReturn(sampleCard());

        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.front").exists());
    }

    @Test
    void create_blankFront_returns400() throws Exception {
        UUID conceptId = UUID.randomUUID();
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conceptId\":\"" + conceptId + "\",\"front\":\"\",\"back\":\"answer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void create_nullConceptId_returns400() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"Q\",\"back\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Update ---

    @Test
    void update_validRequest_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        UpdateCardRequest request = new UpdateCardRequest("New front", "New back", CardType.COMPARE, null, null);

        when(cardService.updateCard(eq(cardId), any(UpdateCardRequest.class), any(User.class))).thenReturn(sampleCard());

        mockMvc.perform(put("/api/cards/" + cardId)
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // --- Delete ---

    @Test
    void delete_existingCard_returns204() throws Exception {
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(delete("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        UUID cardId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Card not found"))
                .when(cardService).deleteCard(eq(cardId), any(User.class));

        mockMvc.perform(delete("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNotFound());
    }

    // --- Switch Algorithm ---

    @Test
    void switchAlgorithm_validRequest_returns200() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.switchAlgorithm(eq(cardId), eq(SchedulingAlgorithm.FSRS), any(User.class)))
                .thenReturn(sampleCard());

        mockMvc.perform(put("/api/cards/" + cardId + "/algorithm")
                        .param("algorithm", "FSRS")
                        .with(withUser(mockUser)))
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
