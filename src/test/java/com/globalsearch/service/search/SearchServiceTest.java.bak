package com.globalsearch.service.search;

import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.LocationDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.document.ZoneDocument;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.response.GlobalSearchResponse;
import com.globalsearch.entity.User;
import com.globalsearch.repository.search.CompanySearchRepository;
import com.globalsearch.repository.search.LocationSearchRepository;
import com.globalsearch.repository.search.SensorSearchRepository;
import com.globalsearch.repository.search.ZoneSearchRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private SearchService searchService;

    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .username("regularuser")
                .email("user@example.com")
                .tenantId("TENANT_A")
                .roles(Set.of(User.Role.VIEWER))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@example.com")
                .tenantId("TENANT_A")
                .roles(Set.of(User.Role.SUPER_ADMIN))
                .enabled(true)
                .build();
    }

    @Test
    void testGlobalSearch_ReturnsResultsForUserTenant() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Test")
                .page(0)
                .size(20)
                .build();

        CompanyDocument company1 = CompanyDocument.builder()
                .id(1L)
                .name("Test Company")
                .tenantId("TENANT_A")
                .industry("Tech")
                .status("ACTIVE")
                .build();

        SensorDocument sensor1 = SensorDocument.builder()
                .id(1L)
                .name("Test Sensor")
                .serialNumber("SN-001")
                .sensorType("TEMPERATURE")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .build();

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Test"))
                .thenReturn(Arrays.asList(company1));
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Test"))
                .thenReturn(Arrays.asList(sensor1));
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(Arrays.asList());

        // Act
        GlobalSearchResponse response = searchService.globalSearch(request, regularUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getTotalResults()).isEqualTo(2);
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getSearchDurationMs()).isGreaterThan(0);

        verify(companySearchRepository).findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Test");
        verify(sensorSearchRepository).findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Test");
    }

    @Test
    void testGlobalSearch_WithEntityTypeFilter() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Sensor")
                .entityTypes(List.of("sensors"))
                .page(0)
                .size(20)
                .build();

        SensorDocument sensor1 = SensorDocument.builder()
                .id(1L)
                .name("Sensor A")
                .serialNumber("SN-001")
                .sensorType("TEMPERATURE")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .build();

        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Sensor"))
                .thenReturn(Arrays.asList(sensor1));

        // Act
        GlobalSearchResponse response = searchService.globalSearch(request, regularUser);

        // Assert
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getEntityType()).isEqualTo("SENSOR");

        // Verify only sensor repo was called
        verify(sensorSearchRepository).findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Sensor");
        verifyNoInteractions(companySearchRepository, locationSearchRepository, zoneSearchRepository);
    }

    @Test
    void testGlobalSearch_Pagination() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Test")
                .page(1)
                .size(2)
                .build();

        List<CompanyDocument> companies = Arrays.asList(
                createCompanyDoc(1L, "Company 1"),
                createCompanyDoc(2L, "Company 2"),
                createCompanyDoc(3L, "Company 3"),
                createCompanyDoc(4L, "Company 4"),
                createCompanyDoc(5L, "Company 5")
        );

        when(companySearchRepository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Test"))
                .thenReturn(companies);
        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(Arrays.asList());
        when(locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(anyString(), anyString()))
                .thenReturn(Arrays.asList());

        // Act
        GlobalSearchResponse response = searchService.globalSearch(request, regularUser);

        // Assert
        assertThat(response.getTotalResults()).isEqualTo(5);
        assertThat(response.getCurrentPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getResults()).hasSize(2); // Page size is 2
    }

    @Test
    void testAdminGlobalSearch_Success() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Test")
                .page(0)
                .size(20)
                .build();

        CompanyDocument company1 = createCompanyDoc(1L, "Test Company");

        when(companySearchRepository.findByNameContainingIgnoreCase("Test"))
                .thenReturn(Arrays.asList(company1));
        when(sensorSearchRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Arrays.asList());
        when(locationSearchRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByNameContainingIgnoreCase(anyString()))
                .thenReturn(Arrays.asList());

        // Act
        GlobalSearchResponse response = searchService.adminGlobalSearch(request, adminUser);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getResults()).hasSize(1);

        // Verify admin search methods were called (no tenant restriction)
        verify(companySearchRepository).findByNameContainingIgnoreCase("Test");
    }

    @Test
    void testAdminGlobalSearch_ThrowsSecurityException_ForNonAdmin() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Test")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> searchService.adminGlobalSearch(request, regularUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("admin privileges");
    }

    @Test
    void testGlobalSearch_EmptyQuery() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("")
                .page(0)
                .size(20)
                .build();

        when(companySearchRepository.findByTenantId("TENANT_A"))
                .thenReturn(Arrays.asList());
        when(sensorSearchRepository.findByTenantId("TENANT_A"))
                .thenReturn(Arrays.asList());
        when(locationSearchRepository.findByTenantId("TENANT_A"))
                .thenReturn(Arrays.asList());
        when(zoneSearchRepository.findByTenantId("TENANT_A"))
                .thenReturn(Arrays.asList());

        // Act
        GlobalSearchResponse response = searchService.globalSearch(request, regularUser);

        // Assert
        assertThat(response).isNotNull();
        verify(companySearchRepository).findByTenantId("TENANT_A");
    }

    @Test
    void testGlobalSearch_WithSensorTypeFilter() {
        // Arrange
        GlobalSearchRequest request = GlobalSearchRequest.builder()
                .query("Sensor")
                .entityTypes(List.of("sensors"))
                .sensorType("TEMPERATURE")
                .page(0)
                .size(20)
                .build();

        SensorDocument tempSensor = SensorDocument.builder()
                .id(1L)
                .name("Temperature Sensor")
                .serialNumber("SN-001")
                .sensorType("TEMPERATURE")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .build();

        SensorDocument humiditySensor = SensorDocument.builder()
                .id(2L)
                .name("Humidity Sensor")
                .serialNumber("SN-002")
                .sensorType("HUMIDITY")
                .tenantId("TENANT_A")
                .status("ACTIVE")
                .build();

        when(sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase("TENANT_A", "Sensor"))
                .thenReturn(Arrays.asList(tempSensor, humiditySensor));

        // Act
        GlobalSearchResponse response = searchService.globalSearch(request, regularUser);

        // Assert - Should only return temperature sensor
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getMetadata().get("sensorType")).isEqualTo("TEMPERATURE");
    }

    // Helper methods
    private CompanyDocument createCompanyDoc(Long id, String name) {
        return CompanyDocument.builder()
                .id(id)
                .name(name)
                .tenantId("TENANT_A")
                .industry("Tech")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
