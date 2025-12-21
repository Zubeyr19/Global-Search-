package com.globalsearch.service.search;

import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.LocationDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.response.GlobalSearchResponse;
import com.globalsearch.entity.User;
import com.globalsearch.repository.search.*;
import com.globalsearch.service.AuditLogService;
import com.globalsearch.service.NotificationService;
import com.globalsearch.service.PerformanceMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SearchService
 * Tests core search logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private CompanySearchRepository companySearchRepository;

    @Mock
    private LocationSearchRepository locationSearchRepository;

    @Mock
    private ZoneSearchRepository zoneSearchRepository;

    @Mock
    private SensorSearchRepository sensorSearchRepository;

    @Mock
    private ReportSearchRepository reportSearchRepository;

    @Mock
    private DashboardSearchRepository dashboardSearchRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PerformanceMetricsService performanceMetricsService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private SearchService searchService;

    private User testUser;
    private GlobalSearchRequest searchRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .tenantId("TENANT_TEST")
                .roles(Set.of(User.Role.VIEWER))
                .build();

        searchRequest = GlobalSearchRequest.builder()
                .query("Tech")
                .page(0)
                .size(20)
                .enableFuzzySearch(false)  // Disable fuzzy search for tests
                .enableSynonyms(false)     // Disable synonyms for tests
                .build();
    }

    @Test
    void testGlobalSearch_Success_ReturnsResults() {
        // Given
        CompanyDocument company = CompanyDocument.builder()
                .id(1L)
                .name("Tech Corp")
                .tenantId("TENANT_TEST")
                .description("Technology company")
                .industry("Technology")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(company));
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getName()).isEqualTo("Tech Corp");
        assertThat(response.getResults().get(0).getEntityType()).isEqualTo("Company");
        assertThat(response.getSearchDurationMs()).isGreaterThanOrEqualTo(0);

        // Verify interactions
        verify(companySearchRepository).findByTenantIdAndNameContainingIgnoreCase("TENANT_TEST", "Tech");
        verify(auditLogService).logSearchEvent(eq(1L), eq("testuser"), eq("TENANT_TEST"),
                eq("Tech"), eq(1), any(HttpServletRequest.class));
        verify(performanceMetricsService).recordQueryExecution(eq("TENANT_TEST"),
                eq("global_search"), anyLong());
        verify(notificationService).notifyUser(eq(1L), any());
    }

    @Test
    void testGlobalSearch_NoResults_ReturnsEmptyList() {
        // Given
        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();

        // Verify audit logging still occurs
        verify(auditLogService).logSearchEvent(eq(1L), eq("testuser"), eq("TENANT_TEST"),
                eq("Tech"), eq(0), any(HttpServletRequest.class));
    }

    @Test
    void testGlobalSearch_MultipleEntityTypes_ReturnsAllResults() {
        // Given
        CompanyDocument company = CompanyDocument.builder()
                .id(1L)
                .name("Tech Solutions")
                .tenantId("TENANT_TEST")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();

        LocationDocument location = LocationDocument.builder()
                .id(2L)
                .name("Tech Building")
                .tenantId("TENANT_TEST")
                .companyId(1L)  // Required field for Map.of() in toSearchResultItem
                .city("Copenhagen")
                .country("Denmark")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();

        SensorDocument sensor = SensorDocument.builder()
                .id(3L)
                .name("Tech Sensor A1")
                .tenantId("TENANT_TEST")
                .serialNumber("SN-001")  // Required field for Map.of() in toSearchResultItem
                .sensorType("TEMPERATURE")
                .status("ACTIVE")
                .build();

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(company));
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(location));
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(sensor));
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(3);
        assertThat(response.getResults()).hasSize(3);

        // Verify all entity types are present
        List<String> entityTypes = response.getResults().stream()
                .map(GlobalSearchResponse.SearchResultItem::getEntityType)
                .toList();
        assertThat(entityTypes).contains("Company", "Location", "Sensor");
    }

    @Test
    void testGlobalSearch_Pagination_ReturnsCorrectPage() {
        // Given - Create 25 results
        List<CompanyDocument> companies = Arrays.asList(
                createCompany(1L, "Company 1"),
                createCompany(2L, "Company 2"),
                createCompany(3L, "Company 3"),
                createCompany(4L, "Company 4"),
                createCompany(5L, "Company 5")
        );

        List<LocationDocument> locations = Arrays.asList(
                createLocation(6L, "Location 1"),
                createLocation(7L, "Location 2"),
                createLocation(8L, "Location 3"),
                createLocation(9L, "Location 4"),
                createLocation(10L, "Location 5")
        );

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(companies);
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(locations);
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // Request page 1 with size 5
        searchRequest.setPage(1);
        searchRequest.setSize(5);

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalResults()).isEqualTo(10);
        assertThat(response.getResults()).hasSize(5);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testGlobalSearch_TenantIsolation_OnlyReturnsTenantData() {
        // Given - Setup repository to verify tenant filtering
        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                eq("TENANT_TEST"), anyString())).thenReturn(Arrays.asList(createCompany(1L, "Test Co")));
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response).isNotNull();

        // Verify all repository calls use correct tenant ID
        verify(companySearchRepository).findByTenantIdAndNameContainingIgnoreCase(
                eq("TENANT_TEST"), anyString());
        verify(locationSearchRepository).findByTenantIdAndNameContainingIgnoreCase(
                eq("TENANT_TEST"), anyString());
        verify(sensorSearchRepository).findByTenantIdAndNameContainingIgnoreCase(
                eq("TENANT_TEST"), anyString());
    }

    @Test
    void testGlobalSearch_PerformanceMetrics_RecordsExecutionTime() {
        // Given
        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response.getSearchDurationMs()).isGreaterThanOrEqualTo(0);

        // Verify performance metrics were recorded
        verify(performanceMetricsService).recordQueryExecution(
                eq("TENANT_TEST"),
                eq("global_search"),
                anyLong()
        );
    }

    @Test
    void testGlobalSearch_RelevanceScoring_SortsResultsByScore() {
        // Given - Create companies with different relevance (exact vs partial match)
        CompanyDocument exactMatch = createCompany(1L, "Tech"); // Exact match = higher score
        CompanyDocument partialMatch = createCompany(2L, "Tech Solutions Inc"); // Partial match = lower score

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(partialMatch, exactMatch));
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());
        when(dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList());

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response.getResults()).hasSize(2);

        // Verify results are sorted by relevance (descending)
        assertThat(response.getResults().get(0).getRelevanceScore())
                .isGreaterThanOrEqualTo(response.getResults().get(1).getRelevanceScore());
    }

    @Test
    void testGlobalSearch_EntityTypeFilter_OnlySearchesSpecifiedTypes() {
        // Given - Request to search only companies
        searchRequest.setEntityTypes(Arrays.asList("companies"));

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                anyString(), anyString())).thenReturn(Arrays.asList(createCompany(1L, "Tech Corp")));

        // When
        GlobalSearchResponse response = searchService.globalSearch(searchRequest, testUser, httpRequest);

        // Then
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getEntityType()).isEqualTo("Company");

        // Verify only company repository was called
        verify(companySearchRepository).findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString());
        verify(locationSearchRepository, never()).findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString());
        verify(sensorSearchRepository, never()).findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString());
    }

    // Helper methods
    private CompanyDocument createCompany(Long id, String name) {
        return CompanyDocument.builder()
                .id(id)
                .name(name)
                .tenantId("TENANT_TEST")
                .status("ACTIVE")
                .industry("Technology")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();
    }

    private LocationDocument createLocation(Long id, String name) {
        return LocationDocument.builder()
                .id(id)
                .name(name)
                .tenantId("TENANT_TEST")
                .companyId(1L)  // Required field for Map.of() in toSearchResultItem
                .city("Copenhagen")
                .country("Denmark")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().toLocalDate())
                .build();
    }
}
