package com.loopy.auth.controller;

// Dependencies: @WebMvcTest, @MockBean, MockMvc, @WithMockUser, SecurityMockMvcRequestPostProcessors — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.dto.*;
import com.loopy.auth.entity.Role;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for AuthController using @WebMvcTest.
 * Loads only the web layer — services are mocked.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // --- Register ---

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123");
        TokenResponse response = new TokenResponse("access-token", "refresh-token", 900000L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(900000));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("dupe@example.com", "password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Login ---

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        TokenResponse response = new TokenResponse("access-token", "refresh-token", 900000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    // --- Refresh ---

    @Test
    void refresh_validToken_returns200() throws Exception {
        RefreshRequest request = new RefreshRequest("valid-refresh-token");
        TokenResponse response = new TokenResponse("new-access-token", "new-refresh-token", 900000L);

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void refresh_invalidToken_returns400() throws Exception {
        RefreshRequest request = new RefreshRequest("invalid-token");

        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Logout ---

    @Test
    void logout_validRequest_returns204() throws Exception {
        RefreshRequest request = new RefreshRequest("some-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout("some-token");
    }

    // --- Me ---

    @Test
    void me_authenticated_returns200() throws Exception {
        User meUser = com.loopy.config.TestSecurityHelper.createTestUser();

        mockMvc.perform(get("/api/auth/me")
                        .with(withUser(meUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    // --- Malformed JSON ---

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
    }

    private void setId(Object entity, UUID id) {
        setField(entity, "id", id);
    }

    private void setField(Object entity, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
