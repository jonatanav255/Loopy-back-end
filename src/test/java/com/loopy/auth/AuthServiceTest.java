package com.loopy.auth;

import com.loopy.auth.dto.LoginRequest;
import com.loopy.auth.dto.RefreshRequest;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.auth.dto.TokenResponse;
import com.loopy.auth.entity.RefreshToken;
import com.loopy.auth.entity.User;
import com.loopy.auth.repository.RefreshTokenRepository;
import com.loopy.auth.repository.UserRepository;
import com.loopy.auth.service.AuthService;
import com.loopy.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — no Spring context, all dependencies mocked.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtService,
                passwordEncoder,
                authenticationManager,
                2592000000L // 30 days refresh expiration
        );
    }

    @Test
    void register_createsUser_hashesPassword_returnsTokens() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token-123");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token-123", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(900000L, response.expiresIn());

        // Verify password was hashed before saving
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashed_password", userCaptor.getValue().getPassword());
        assertEquals("user@example.com", userCaptor.getValue().getEmail());

        // Verify refresh token was persisted
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );
        assertEquals("Email already registered", exception.getMessage());

        // Should never save anything
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_correctCredentials_returnsTokens() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        User user = new User("user@example.com", "hashed_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@example.com", "password123"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("login-access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("login-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(900000L, response.expiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        // Should never look up user or create tokens
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_nonExistentUser_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("nobody@example.com", "password123");

        // AuthenticationManager throws BadCredentialsException for non-existent users too
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void logout_invalidatesRefreshToken() {
        String tokenValue = "refresh-token-to-revoke";
        RefreshToken refreshToken = mock(RefreshToken.class);

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(refreshToken));

        authService.logout(tokenValue);

        verify(refreshToken).setRevoked(true);
        verify(refreshTokenRepository).save(refreshToken);
    }

    @Test
    void logout_unknownToken_doesNothing() {
        String tokenValue = "unknown-token";
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        // Should not throw
        authService.logout(tokenValue);

        verify(refreshTokenRepository, never()).save(any());
    }

    // --- refresh ---

    @Test
    void refresh_validToken_revokesOldAndIssuesNewTokens() {
        String oldTokenValue = "old-refresh-token";
        User user = new User("user@example.com", "hashed");
        setId(user, UUID.randomUUID());

        RefreshToken oldToken = new RefreshToken(user, oldTokenValue, Instant.now().plusMillis(86400000));
        setId(oldToken, UUID.randomUUID());

        RefreshRequest request = new RefreshRequest(oldTokenValue);

        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);

        TokenResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotEquals(oldTokenValue, response.refreshToken());
        assertEquals(900000L, response.expiresIn());

        // Old token should be revoked
        assertTrue(oldToken.isRevoked());

        // Should save old revoked token + new token
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_invalidToken_throwsException() {
        RefreshRequest request = new RefreshRequest("nonexistent-token");

        when(refreshTokenRepository.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(request)
        );
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_expiredToken_throwsException() {
        String tokenValue = "expired-refresh-token";
        User user = new User("user@example.com", "hashed");
        setId(user, UUID.randomUUID());

        // Expired token — expiry is in the past
        RefreshToken expiredToken = new RefreshToken(user, tokenValue, Instant.now().minusMillis(86400000));
        setId(expiredToken, UUID.randomUUID());

        RefreshRequest request = new RefreshRequest(tokenValue);

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(request)
        );
        assertEquals("Refresh token is expired or revoked", exception.getMessage());

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_revokedToken_throwsException() {
        String tokenValue = "revoked-refresh-token";
        User user = new User("user@example.com", "hashed");
        setId(user, UUID.randomUUID());

        RefreshToken revokedToken = new RefreshToken(user, tokenValue, Instant.now().plusMillis(86400000));
        setId(revokedToken, UUID.randomUUID());
        revokedToken.setRevoked(true);

        RefreshRequest request = new RefreshRequest(tokenValue);

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(revokedToken));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(request)
        );
        assertEquals("Refresh token is expired or revoked", exception.getMessage());

        verify(jwtService, never()).generateAccessToken(any());
    }

    /** Reflection helper to set UUID id on entities with private id field. */
    private void setId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}
