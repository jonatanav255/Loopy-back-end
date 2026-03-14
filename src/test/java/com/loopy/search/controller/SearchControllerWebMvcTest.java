package com.loopy.search.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.search.dto.CardSearchResult;
import com.loopy.search.dto.ConceptSearchResult;
import com.loopy.search.dto.SearchResponse;
import com.loopy.search.dto.TopicSearchResult;
import com.loopy.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for SearchController using @WebMvcTest.
 */
@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class SearchControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    @Test
    void search_withQuery_returns200() throws Exception {
        TopicSearchResult topicResult = new TopicSearchResult(UUID.randomUUID(), "Java", "Programming", "#FF0000");
        SearchResponse response = new SearchResponse(List.of(topicResult), List.of(), List.of());

        when(searchService.search(eq("Java"), any(User.class))).thenReturn(response);

        mockMvc.perform(get("/api/search")
                        .param("q", "Java")
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics[0].name").value("Java"))
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_emptyQuery_returnsEmptyResults() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "")
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty())
                .andExpect(jsonPath("$.concepts").isEmpty())
                .andExpect(jsonPath("$.cards").isEmpty());
    }

    @Test
    void search_whitespaceOnlyQuery_returnsEmptyResults() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "   ")
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty());
    }

    @Test
    void search_noQueryParam_returnsEmptyResults() throws Exception {
        mockMvc.perform(get("/api/search")
                        .with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topics").isEmpty());
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
