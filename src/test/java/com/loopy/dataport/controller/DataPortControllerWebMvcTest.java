package com.loopy.dataport.controller;

// Dependencies: @WebMvcTest, @MockBean, @AutoConfigureMockMvc, MockMvc, JpaMetamodelMappingContext — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.auth.entity.User;
import com.loopy.auth.filter.JwtAuthenticationFilter;
import com.loopy.dataport.dto.*;
import com.loopy.dataport.service.DataPortService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.loopy.config.TestSecurityHelper.withUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for DataPortController using @WebMvcTest.
 */
@WebMvcTest(DataPortController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(JpaMetamodelMappingContext.class)
class DataPortControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataPortService dataPortService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final User mockUser = createUser();

    private static User createUser() {
        User u = new User("user@example.com", "hashed");
        setId(u, UUID.randomUUID());
        return u;
    }

    // --- Export ---

    @Test
    void export_returns200() throws Exception {
        ExportResponse response = new ExportResponse("1.0", Instant.now(), 1, 2, 5, List.of());
        when(dataPortService.exportData(any(User.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/dataport/export").with(withUser(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportVersion").value("1.0"))
                .andExpect(jsonPath("$.topicCount").value(1));
    }

    @Test
    void export_withTopicIds_returns200() throws Exception {
        UUID topicId = UUID.randomUUID();
        ExportResponse response = new ExportResponse("1.0", Instant.now(), 1, 1, 2, List.of());
        when(dataPortService.exportData(any(User.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/dataport/export")
                        .param("topicIds", topicId.toString())
                        .with(withUser(mockUser)))
                .andExpect(status().isOk());
    }

    // --- Import ---

    @Test
    void importData_validRequest_returns201() throws Exception {
        ImportTopicData topicData = new ImportTopicData("Java", "desc", "#FF0000",
                List.of(new ImportConceptData("OOP", "notes", null,
                        List.of(new ImportCardData("Q", "A", "STANDARD", null, null)))));
        ImportRequest request = new ImportRequest("1.0", List.of(topicData));
        ImportResponse response = new ImportResponse(1, 1, 1);

        when(dataPortService.importData(any(ImportRequest.class), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/dataport/import")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicsCreated").value(1))
                .andExpect(jsonPath("$.conceptsCreated").value(1))
                .andExpect(jsonPath("$.cardsCreated").value(1));
    }

    @Test
    void importData_emptyTopics_returns400() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exportVersion\":\"1.0\",\"topics\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void importData_nullExportVersion_returns400() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topics\":[{\"name\":\"Test\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void importData_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/dataport/import")
                        .with(withUser(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed request body"));
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
