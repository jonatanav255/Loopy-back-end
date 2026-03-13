package com.loopy.auth;

import com.loopy.auth.entity.User;
import com.loopy.auth.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService — no Spring context, direct instantiation.
 */
class JwtServiceTest {

    private JwtService jwtService;

    // Must be at least 256 bits (32 bytes) for HMAC-SHA256
    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256!!";
    private static final long ACCESS_EXPIRATION_MS = 900000L; // 15 minutes

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ACCESS_EXPIRATION_MS);
    }

    @Test
    void generateAccessToken_containsCorrectSubject() {
        User user = new User("alice@example.com", "hashed");

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedEmail = jwtService.extractEmail(token);
        assertEquals("alice@example.com", extractedEmail);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        User user = new User("bob@example.com", "hashed");
        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Create a JwtService with 0ms expiration to generate an immediately expired token
        JwtService expiredJwtService = new JwtService(SECRET, 0L);
        User user = new User("charlie@example.com", "hashed");

        String token = expiredJwtService.generateAccessToken(user);

        // Expired token should throw when extracting claims
        assertThrows(ExpiredJwtException.class, () -> expiredJwtService.extractEmail(token));
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        User user = new User("diana@example.com", "hashed");
        String token = jwtService.generateAccessToken(user);

        assertEquals("diana@example.com", jwtService.extractEmail(token));
    }

    @Test
    void differentTokens_forDifferentUsers() {
        User alice = new User("alice@example.com", "hashed");
        User bob = new User("bob@example.com", "hashed");

        String aliceToken = jwtService.generateAccessToken(alice);
        String bobToken = jwtService.generateAccessToken(bob);

        assertNotEquals(aliceToken, bobToken);
        assertEquals("alice@example.com", jwtService.extractEmail(aliceToken));
        assertEquals("bob@example.com", jwtService.extractEmail(bobToken));
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        User alice = new User("alice@example.com", "hashed");
        User bob = new User("bob@example.com", "hashed");

        String aliceToken = jwtService.generateAccessToken(alice);

        // Token generated for alice should not be valid for bob
        assertFalse(jwtService.isTokenValid(aliceToken, bob));
    }

    @Test
    void getAccessExpirationMs_returnsConfiguredValue() {
        assertEquals(ACCESS_EXPIRATION_MS, jwtService.getAccessExpirationMs());
    }

    @Test
    void malformedToken_throwsException() {
        assertThrows(Exception.class, () -> jwtService.extractEmail("not.a.valid.jwt"));
    }
}
