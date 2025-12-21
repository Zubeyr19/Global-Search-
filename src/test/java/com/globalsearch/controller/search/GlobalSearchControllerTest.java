package com.globalsearch.controller.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.request.LoginRequest;
import com.globalsearch.dto.response.LoginResponse;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.User;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.repository.UserRepository;
import com.globalsearch.repository.search.CompanySearchRepository;
import com.globalsearch.repository.search.SensorSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("Integration tests require Elasticsearch to be running. Enable when Elasticsearch is available.")
class GlobalSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySearchRepository companySearchRepository;

    @Autowired
    private SensorSearchRepository sensorSearchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private Company testCompany;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        userRepository.deleteAll();
        companyRepository.deleteAll();
        companySearchRepository.deleteAll();
        sensorSearchRepository.deleteAll();

        // Create test company
        testCompany = Company.builder()
                .name("Tech Corp")
                .tenantId("TENANT_TECH")
                .industry("Technology")
                .status(Company.CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create regular user
        User regularUser = User.builder()
                .username("regularuser")
                .password(passwordEncoder.encode("password123"))
                .email("user@example.com")
                .firstName("Regular")
                .lastName("User")
                .tenantId("TENANT_TECH")
                .companyId(testCompany.getId())
                .roles(Set.of(User.Role.VIEWER))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(regularUser);

        // Create admin user
        User adminUser = User.builder()
                .username("adminuser")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .tenantId("TENANT_TECH")
                .companyId(testCompany.getId())
                .roles(Set.of(User.Role.SUPER_ADMIN))
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(adminUser);

        // Index test company in Elasticsearch
        CompanyDocument companyDoc = CompanyDocument.fromEntity(testCompany);
        companySearchRepository.save(companyDoc);

        // Create test sensors
        SensorDocument sensor1 = SensorDocument.builder()
                .id(1L)
                .name("Temperature Sensor A1")
                .serialNumber("SN-001")
                .sensorType("TEMPERATURE")
                .status("ACTIVE")
                .tenantId("TENANT_TECH")
                .build();
        sensorSearchRepository.save(sensor1);

        SensorDocument sensor2 = SensorDocument.builder()
                .id(2L)
                .name("Humidity Sensor B1")
                .serialNumber("SN-002")
                .sensorType("HUMIDITY")
                .status("ACTIVE")
                .tenantId("TENANT_TECH")
                .build();
        sensorSearchRepository.save(sensor2);

        // Get tokens
        userToken = loginAndGetToken("regularuser", "password123");
        adminToken = loginAndGetToken("adminuser", "admin123");

        // Wait for Elasticsearch to index
        Thread.sleep(1000);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        return response.getAccessToken();
    }

    @Test
    void testGlobalSearch_Success() throws Exception {
        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                .query("Tech")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.totalResults").exists())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.searchDurationMs").exists());
    }

    @Test
    void testGlobalSearch_Unauthorized() throws Exception {
        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                .query("Tech")
                .build();

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testQuickSearch_Success() throws Exception {
        mockMvc.perform(get("/api/search/quick")
                        .param("query", "Sensor")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testSearchByEntityType_Success() throws Exception {
        mockMvc.perform(get("/api/search/sensors")
                        .param("query", "Temperature")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testAdminSearch_Success() throws Exception {
        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                .query("Tech")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search/admin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.searchDurationMs").exists());
    }

    @Test
    void testAdminSearch_Forbidden_ForRegularUser() throws Exception {
        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                .query("Tech")
                .build();

        mockMvc.perform(post("/api/search/admin")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Admin privileges required"));
    }

    @Test
    void testSearchStats_Success() throws Exception {
        mockMvc.perform(get("/api/search/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.tenantId").value("TENANT_TECH"));
    }

    @Test
    void testSearchWithFilters_Success() throws Exception {
        GlobalSearchRequest searchRequest = GlobalSearchRequest.builder()
                .query("Sensor")
                .entityTypes(List.of("sensors"))
                .sensorType("TEMPERATURE")
                .page(0)
                .size(20)
                .build();

        mockMvc.perform(post("/api/search")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray());
    }
}
