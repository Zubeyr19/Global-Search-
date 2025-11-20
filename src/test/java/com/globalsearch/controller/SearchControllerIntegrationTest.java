package com.globalsearch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.entity.User;
import com.globalsearch.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Search endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .tenantId("TEST_TENANT")
                .roles(java.util.Set.of(User.Role.VIEWER))
                .build();

        when(userDetailsService.loadUserEntityByUsername(anyString()))
                .thenReturn(testUser);
    }

    @Test
    @DisplayName("Should require authentication for search endpoint")
    void testSearchRequiresAuthentication() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("test")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should allow authenticated user to search")
    void testAuthenticatedSearch() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists())
                .andExpect(jsonPath("$.totalResults").exists())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should support quick search")
    void testQuickSearch() throws Exception {
        mockMvc.perform(get("/api/search/quick")
                        .param("query", "sensor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should filter by entity type")
    void testSearchByEntityType() throws Exception {
        mockMvc.perform(get("/api/search/sensors")
                        .param("query", "temperature")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN"})
    @DisplayName("Should allow admin cross-tenant search")
    void testAdminCrossTenantSearch() throws Exception {
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .tenantId("SYSTEM")
                .roles(java.util.Set.of(User.Role.SUPER_ADMIN))
                .build();

        when(userDetailsService.loadUserEntityByUsername("admin"))
                .thenReturn(adminUser);

        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should deny admin search for non-admin user")
    void testNonAdminCannotAccessAdminSearch() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should get search statistics")
    void testGetSearchStats() throws Exception {
        mockMvc.perform(get("/api/search/stats")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.totalSearches").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should support fuzzy search")
    void testFuzzySearch() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensr") // Misspelled
                .enableFuzzySearch(true)
                .fuzzyMaxEdits(2)
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should support highlighting")
    void testSearchWithHighlighting() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .enableHighlighting(true)
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should support filtering by multiple entity types")
    void testMultipleEntityTypeFilter() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .entityTypes(List.of("Sensor", "Location"))
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should validate pagination parameters")
    void testPaginationValidation() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"VIEWER"})
    @DisplayName("Should sort search results")
    void testSearchSorting() throws Exception {
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("sensor")
                .sortBy("name")
                .sortDirection("ASC")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }
}
