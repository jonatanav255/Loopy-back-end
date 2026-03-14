package com.loopy.config;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.entity.CardType;
import com.loopy.card.controller.CardController;
import com.loopy.card.service.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExceptionHandler.
 * Verifies correct HTTP status codes and error response bodies for various exception types.
 * Uses CardController as a test vehicle since it can trigger all exception types.
 */
@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class GlobalExceptionHandlerTest {

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

    // --- 404 Not Found ---

    @Test
    void resourceNotFound_returns404_withErrorMessage() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Card not found"));
    }

    @Test
    void resourceNotFound_customMessage_preservedInResponse() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Custom not found message"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Custom not found message"));
    }

    // --- 400 Bad Request (IllegalArgumentException) ---

    @Test
    void illegalArgument_returns400_withErrorMessage() throws Exception {
        UUID conceptId = UUID.randomUUID();
        CreateCardRequest request = new CreateCardRequest(conceptId, "Q", "A", CardType.STANDARD, null, null);

        when(cardService.createCard(any(), any(User.class)))
                .thenThrow(new IllegalArgumentException("Duplicate card"));

        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Duplicate card"));
    }

    // --- 400 Bad Request (Validation failure) ---

    @Test
    void validationFailure_returns400_withFieldError() throws Exception {
        // Missing required fields triggers MethodArgumentNotValidException
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"\",\"back\":\"A\",\"conceptId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void validationFailure_multipleErrors_returnsFirstError() throws Exception {
        // Both front and back blank, plus missing conceptId
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"front\":\"\",\"back\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- 400 Bad Request (Malformed JSON) ---

    @Test
    void malformedJson_returns400_withGenericMessage() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    @Test
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    // --- 500 Internal Server Error ---

    @Test
    void runtimeException_returns500_withGenericMessage() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An internal error occurred"));
    }

    @Test
    void runtimeException_doesNotLeakInternalDetails() throws Exception {
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new RuntimeException("Database connection failed: password=secret123"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("An internal error occurred"));
    }

    // --- Error response format ---

    @Test
    void allErrorResponses_useConsistentFormat() throws Exception {
        // Not found
        UUID cardId = UUID.randomUUID();
        when(cardService.getCard(eq(cardId), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").isString())
                .andExpect(jsonPath("$.length()").value(1)); // Only "error" key
    }

    // --- Delete not found ---

    @Test
    void deleteNotFound_returns404() throws Exception {
        UUID cardId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Card not found"))
                .when(cardService).deleteCard(eq(cardId), any(User.class));

        mockMvc.perform(delete("/api/cards/" + cardId).with(withUser(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Card not found"));
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
