package com.loopy.config;

import com.loopy.auth.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

/**
 * Helper for WebMvcTest tests to set up the SecurityContext with a mock user.
 * Needed because @WebMvcTest with addFilters=false does not populate SecurityContextHolder,
 * so @AuthenticationPrincipal resolves to null without this.
 */
public final class TestSecurityHelper {

    private TestSecurityHelper() {}

    /**
     * Returns a RequestPostProcessor that sets the SecurityContext with the given user.
     * Use with mockMvc.perform(get("/...").with(TestSecurityHelper.withUser(user)))
     */
    public static org.springframework.test.web.servlet.request.RequestPostProcessor withUser(User user) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
            );
            return request;
        };
    }

    /**
     * Creates a User with ID and createdAt set for testing.
     */
    public static User createTestUser() {
        return createTestUser("user@example.com");
    }

    public static User createTestUser(String email) {
        User user = new User(email, "hashed");
        setField(user, "id", UUID.randomUUID());
        setField(user, "createdAt", java.time.Instant.now());
        return user;
    }

    private static void setField(Object entity, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set " + fieldName, e);
        }
    }
}
