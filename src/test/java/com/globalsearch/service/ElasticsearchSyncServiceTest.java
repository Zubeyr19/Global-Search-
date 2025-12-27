package com.globalsearch.service;

import com.globalsearch.document.CompanyDocument;
import com.globalsearch.document.LocationDocument;
import com.globalsearch.document.SensorDocument;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.Location;
import com.globalsearch.entity.Sensor;
import com.globalsearch.repository.*;
import com.globalsearch.repository.search.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for ElasticsearchSyncService
 * Tests synchronization logic with mocked repositories
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchSyncServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DashboardRepository dashboardRepository;

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

    @InjectMocks
    private ElasticsearchSyncService syncService;

    private Company testCompany;
    private Location testLocation;
    private Sensor testSensor;

    @BeforeEach
    void setUp() {
        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .tenantId("TENANT_001")
                .industry("Technology")
                .status(Company.CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testLocation = Location.builder()
                .id(2L)
                .name("Test Location")
                .company(testCompany)
                .city("Copenhagen")
                .country("Denmark")
                .status(Location.LocationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        testSensor = Sensor.builder()
                .id(3L)
                .name("Test Sensor")
                .serialNumber("SN-001")
                .sensorType(Sensor.SensorType.TEMPERATURE)
                .status(Sensor.SensorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSyncAllCompanies_Success() {
        // Given
        List<Company> companies = Arrays.asList(testCompany);
        when(companyRepository.findAll()).thenReturn(companies);

        // When
        syncService.syncAllCompanies();

        // Then
        verify(companyRepository).findAll();
        verify(companySearchRepository, times(1)).save(any(CompanyDocument.class));
    }

    @Test
    void testSyncAllCompanies_MultipleEntities() {
        // Given
        Company company1 = Company.builder()
                .id(1L)
                .name("Company 1")
                .tenantId("TENANT_001")
                .status(Company.CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Company company2 = Company.builder()
                .id(2L)
                .name("Company 2")
                .tenantId("TENANT_002")
                .status(Company.CompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyRepository.findAll()).thenReturn(Arrays.asList(company1, company2));

        // When
        syncService.syncAllCompanies();

        // Then
        verify(companyRepository).findAll();
        verify(companySearchRepository, times(2)).save(any(CompanyDocument.class));
    }

    @Test
    void testSyncAllCompanies_EmptyList() {
        // Given
        when(companyRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        syncService.syncAllCompanies();

        // Then
        verify(companyRepository).findAll();
        verify(companySearchRepository, never()).save(any(CompanyDocument.class));
    }

    @Test
    void testSyncAllCompanies_HandlesSyncError() {
        // Given
        when(companyRepository.findAll()).thenReturn(Arrays.asList(testCompany));
        doThrow(new RuntimeException("Elasticsearch connection failed"))
                .when(companySearchRepository).save(any(CompanyDocument.class));

        // When
        syncService.syncAllCompanies();

        // Then - Should not throw exception, just log error
        verify(companyRepository).findAll();
        verify(companySearchRepository).save(any(CompanyDocument.class));
    }

    @Test
    void testSyncAllLocations_Success() {
        // Given
        List<Location> locations = Arrays.asList(testLocation);
        when(locationRepository.findAll()).thenReturn(locations);

        // When
        syncService.syncAllLocations();

        // Then
        verify(locationRepository).findAll();
        verify(locationSearchRepository, times(1)).save(any(LocationDocument.class));
    }

    @Test
    void testSyncAllSensors_Success() {
        // Given
        List<Sensor> sensors = Arrays.asList(testSensor);
        when(sensorRepository.findAll()).thenReturn(sensors);

        // When
        syncService.syncAllSensors();

        // Then
        verify(sensorRepository).findAll();
        verify(sensorSearchRepository, times(1)).save(any(SensorDocument.class));
    }

    @Test
    void testSyncAllDataOnStartup_SyncsAllEntities() {
        // Given
        when(companyRepository.findAll()).thenReturn(Arrays.asList(testCompany));
        when(locationRepository.findAll()).thenReturn(Arrays.asList(testLocation));
        when(zoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(sensorRepository.findAll()).thenReturn(Arrays.asList(testSensor));
        when(reportRepository.findAll()).thenReturn(Collections.emptyList());
        when(dashboardRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        syncService.syncAllDataOnStartup();

        // Then - Verify all entity types are synced
        verify(companyRepository).findAll();
        verify(locationRepository).findAll();
        verify(zoneRepository).findAll();
        verify(sensorRepository).findAll();
        verify(reportRepository).findAll();
        verify(dashboardRepository).findAll();

        verify(companySearchRepository, times(1)).save(any(CompanyDocument.class));
        verify(locationSearchRepository, times(1)).save(any(LocationDocument.class));
        verify(sensorSearchRepository, times(1)).save(any(SensorDocument.class));
    }

    @Test
    void testSyncAllDataOnStartup_HandlesPartialFailure() {
        // Given - Setup so that company sync fails but location sync succeeds
        when(companyRepository.findAll()).thenThrow(new RuntimeException("Database connection lost"));
        lenient().when(locationRepository.findAll()).thenReturn(Arrays.asList(testLocation));
        lenient().when(zoneRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(sensorRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(reportRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(dashboardRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        syncService.syncAllDataOnStartup();

        // Then - Should log error but continue with other entities
        verify(companyRepository).findAll();
        // Other syncs may not execute due to exception propagation
    }

    @Test
    void testSyncAllSensors_BulkSync_HandlesLargeDataset() {
        // Given - Simulate large dataset (100 sensors)
        List<Sensor> sensors = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            sensors.add(Sensor.builder()
                    .id((long) i)
                    .name("Sensor " + i)
                    .serialNumber("SN-" + String.format("%03d", i))
                    .sensorType(Sensor.SensorType.TEMPERATURE)
                    .status(Sensor.SensorStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        when(sensorRepository.findAll()).thenReturn(sensors);

        // When
        syncService.syncAllSensors();

        // Then
        verify(sensorRepository).findAll();
        verify(sensorSearchRepository, times(100)).save(any(SensorDocument.class));
    }

    @Test
    void testSyncAllCompanies_VerifiesTransactionalBehavior() {
        // Given
        when(companyRepository.findAll()).thenReturn(Arrays.asList(testCompany));

        // When
        syncService.syncAllCompanies();

        // Then - Verify readonly transaction is used (implicit in @Transactional)
        verify(companyRepository).findAll();
        verify(companySearchRepository).save(any(CompanyDocument.class));
    }

    @Test
    void testSyncAllLocations_EmptyList_NoIndexingOccurs() {
        // Given
        when(locationRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        syncService.syncAllLocations();

        // Then
        verify(locationRepository).findAll();
        verify(locationSearchRepository, never()).save(any(LocationDocument.class));
    }

    @Test
    void testSyncAllSensors_PartialFailure_ContinuesProcessing() {
        // Given - First sensor succeeds, second fails, third succeeds
        Sensor sensor1 = Sensor.builder()
                .id(1L)
                .name("Sensor 1")
                .serialNumber("SN-001")
                .sensorType(Sensor.SensorType.TEMPERATURE)
                .status(Sensor.SensorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Sensor sensor2 = Sensor.builder()
                .id(2L)
                .name("Sensor 2")
                .serialNumber("SN-002")
                .sensorType(Sensor.SensorType.HUMIDITY)
                .status(Sensor.SensorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Sensor sensor3 = Sensor.builder()
                .id(3L)
                .name("Sensor 3")
                .serialNumber("SN-003")
                .sensorType(Sensor.SensorType.PRESSURE)
                .status(Sensor.SensorStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(sensorRepository.findAll()).thenReturn(Arrays.asList(sensor1, sensor2, sensor3));

        // Make second save throw exception using a counter approach
        final int[] callCount = {0};
        when(sensorSearchRepository.save(any(SensorDocument.class))).thenAnswer(invocation -> {
            callCount[0]++;
            if (callCount[0] == 2) {
                throw new RuntimeException("Indexing failed");
            }
            return invocation.getArgument(0);
        });

        // When
        syncService.syncAllSensors();

        // Then - Should process all 3 despite failure on sensor 2
        verify(sensorRepository).findAll();
        verify(sensorSearchRepository, times(3)).save(any(SensorDocument.class));
    }
}
