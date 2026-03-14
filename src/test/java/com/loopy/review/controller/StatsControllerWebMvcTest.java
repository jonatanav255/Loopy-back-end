package com.loopy.review.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.card.dto.CardResponse;
import com.loopy.review.dto.FragileCard;
import com.loopy.review.dto.HeatmapEntry;
import com.loopy.review.dto.StatsOverview;
import com.loopy.review.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for StatsController using @WebMvcTest.
 */
@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class StatsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    // --- Overview ---

    @Test
    void overview_returns200() throws Exception {
        StatsOverview overview = new StatsOverview(5, 3, 50, 75.0, 7, 14);
        when(statsService.getOverview(any(User.class))).thenReturn(overview);

        mockMvc.perform(get("/api/stats/overview").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsDueToday").value(5))
                .andExpect(jsonPath("$.cardsReviewedToday").value(3))
                .andExpect(jsonPath("$.totalCards").value(50))
                .andExpect(jsonPath("$.accuracyToday").value(75.0))
                .andExpect(jsonPath("$.currentStreak").value(7))
                .andExpect(jsonPath("$.longestStreak").value(14));
    }

    // --- Heatmap ---

    @Test
    void heatmap_returns200() throws Exception {
        HeatmapEntry entry = new HeatmapEntry(LocalDate.of(2025, 1, 15), 5);
        when(statsService.getHeatmap(any(User.class))).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/stats/heatmap").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].count").value(5));
    }

    // --- Fragile ---

    @Test
    void fragile_returns200() throws Exception {
        CardResponse card = new CardResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Q", "A", "STANDARD",
                null, null, 3, 2.5, 1, LocalDate.now(), LocalDate.now().minusDays(1),
                0, 0, "SM2", Instant.now(), Instant.now()
        );
        FragileCard fragile = new FragileCard(card, 3, 1, 2);
        when(statsService.getFragileCards(any(User.class))).thenReturn(List.of(fragile));

        mockMvc.perform(get("/api/stats/fragile").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastRating").value(3))
                .andExpect(jsonPath("$[0].lastConfidence").value(1))
                .andExpect(jsonPath("$[0].occurrences").value(2));
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
