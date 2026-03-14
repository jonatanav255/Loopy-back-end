package com.loopy.auth.repository;

// Dependencies: @DataJpaTest, TestEntityManager — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for UserRepository using @DataJpaTest.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_existingUser_returns() {
        User user = new User("test@example.com", "hashed");
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_nonExistent_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_existingUser_returnsTrue() {
        entityManager.persist(new User("exists@example.com", "hashed"));
        entityManager.flush();

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void existsByEmail_nonExistent_returnsFalse() {
        assertFalse(userRepository.existsByEmail("nobody@example.com"));
    }

    @Test
    void save_persistsUserWithDefaults() {
        User user = new User("new@example.com", "hashed");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("USER", saved.getRole().name());
        assertTrue(saved.isEnabled());
    }
}
