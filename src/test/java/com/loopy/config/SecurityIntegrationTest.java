package com.loopy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.auth.service.JwtService;
import com.loopy.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security integration tests — verifies authentication/authorization rules and CORS.
 * Uses full Spring context with H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Test
    void protectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-jwt-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withRandomBearerString_returns403() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns403() throws Exception {
        // Create a JwtService with 0ms expiration
        JwtService expiredJwtService = new JwtService(
                "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!!", 0L);
        User user = new User("expired@example.com", "hashed");
        String expiredToken = expiredJwtService.generateAccessToken(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicEndpoint_register_accessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("security-test@example.com", "password123"))))
                .andExpect(status().isCreated());
    }

    @Test
    void publicEndpoint_login_accessibleWithoutToken() throws Exception {
        // First register
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("security-login@example.com", "password123"))))
                .andExpect(status().isCreated());

        // Login should be accessible without token
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"security-login@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_topics_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_reviews_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/reviews/due"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_stats_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withValidToken_returns200() throws Exception {
        // Register to get a valid token
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("sec-valid@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();

        TokenResponse tokens = objectMapper.readValue(
                result.getResponse().getContentAsString(), TokenResponse.class);

        // Use valid token to access protected endpoint
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sec-valid@example.com"));
    }

    @Test
    void cors_preflightRequest_returnsAllowHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void authorizationHeader_noBearerPrefix_treatedAsNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Basic some-credentials"))
                .andExpect(status().isForbidden());
    }
}
