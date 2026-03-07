package com.loopy.auth.service;

import com.loopy.auth.dto.*;
import com.loopy.auth.entity.RefreshToken;
import com.loopy.auth.entity.User;
import com.loopy.auth.repository.RefreshTokenRepository;
import com.loopy.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Core authentication service — handles register, login, token refresh, and logout.
 * Uses a dual-token strategy:
 *   - Access token (JWT, short-lived ~15min) for API authorization
 *   - Refresh token (opaque UUID, long-lived ~30 days) stored in DB for rotation
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final long refreshExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /** Creates a new user with BCrypt-hashed password, returns both tokens. */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        userRepository.save(user);

        return createTokens(user);
    }

    /**
     * Authenticates user via Spring Security's AuthenticationManager.
     * Throws BadCredentialsException (caught by GlobalExceptionHandler) if password is wrong.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return createTokens(user);
    }

    /**
     * Token rotation: validates the old refresh token, revokes it, and issues a new pair.
     * This prevents replay attacks — each refresh token can only be used once.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new IllegalArgumentException("Refresh token is expired or revoked");
        }

        // Revoke the old token before issuing new ones
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return createTokens(refreshToken.getUser());
    }

    /** Revokes the refresh token so it can't be used again. */
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /** Generates a new access token (JWT) + refresh token (UUID) pair and persists the refresh token. */
    private TokenResponse createTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenValue,
                Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue, jwtService.getAccessExpirationMs());
    }
}
