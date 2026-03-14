package com.loopy.auth.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.RefreshToken;
import com.loopy.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for RefreshTokenRepository using @DataJpaTest.
 */
@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "hashed");
        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    void findByToken_existingToken_returns() {
        RefreshToken token = new RefreshToken(user, "test-token-value", Instant.now().plusMillis(86400000));
        entityManager.persist(token);
        entityManager.flush();

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("test-token-value");

        assertTrue(found.isPresent());
        assertEquals("test-token-value", found.get().getToken());
    }

    @Test
    void findByToken_nonExistent_returnsEmpty() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    @Transactional
    void revokeAllByUserId_revokesAllActiveTokens() {
        RefreshToken token1 = new RefreshToken(user, "token-1", Instant.now().plusMillis(86400000));
        RefreshToken token2 = new RefreshToken(user, "token-2", Instant.now().plusMillis(86400000));
        entityManager.persist(token1);
        entityManager.persist(token2);
        entityManager.flush();

        refreshTokenRepository.revokeAllByUserId(user.getId());
        entityManager.flush();
        entityManager.clear();

        // Both tokens should now be revoked
        RefreshToken found1 = entityManager.find(RefreshToken.class, token1.getId());
        RefreshToken found2 = entityManager.find(RefreshToken.class, token2.getId());

        assertTrue(found1.isRevoked());
        assertTrue(found2.isRevoked());
    }

    @Test
    @Transactional
    void revokeAllByUserId_doesNotAffectAlreadyRevokedTokens() {
        RefreshToken activeToken = new RefreshToken(user, "active-token", Instant.now().plusMillis(86400000));
        RefreshToken revokedToken = new RefreshToken(user, "revoked-token", Instant.now().plusMillis(86400000));
        revokedToken.setRevoked(true);

        entityManager.persist(activeToken);
        entityManager.persist(revokedToken);
        entityManager.flush();

        // Should only update the active token
        refreshTokenRepository.revokeAllByUserId(user.getId());
        entityManager.flush();
        entityManager.clear();

        RefreshToken foundActive = entityManager.find(RefreshToken.class, activeToken.getId());
        assertTrue(foundActive.isRevoked());
    }
}
