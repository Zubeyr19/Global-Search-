package com.globalsearch.service;

import com.globalsearch.document.*;
import com.globalsearch.entity.*;
import com.globalsearch.repository.*;
import com.globalsearch.repository.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for synchronizing data from MySQL to Elasticsearch.
 * This ensures that all search indices are up-to-date with the relational database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncService {

    // MySQL Repositories
    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final ReportRepository reportRepository;
    private final DashboardRepository dashboardRepository;

    // Elasticsearch Repositories
    private final CompanySearchRepository companySearchRepository;
    private final LocationSearchRepository locationSearchRepository;
    private final ZoneSearchRepository zoneSearchRepository;
    private final SensorSearchRepository sensorSearchRepository;
    private final ReportSearchRepository reportSearchRepository;
    private final DashboardSearchRepository dashboardSearchRepository;

    /**
     * Synchronizes all data from MySQL to Elasticsearch on application startup.
     * This runs after the application is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void syncAllDataOnStartup() {
        log.info("=================================================================");
        log.info("Starting Elasticsearch data synchronization...");
        log.info("=================================================================");

        long startTime = System.currentTimeMillis();

        try {
            syncAllCompanies();
            syncAllLocations();
            syncAllZones();
            syncAllSensors();
            syncAllReports();
            syncAllDashboards();

            long duration = System.currentTimeMillis() - startTime;
            log.info("=================================================================");
            log.info("Elasticsearch synchronization completed successfully in {} ms", duration);
            log.info("=================================================================");
        } catch (Exception e) {
            log.error("Error during Elasticsearch synchronization", e);
            log.error("Search functionality may be limited until sync is completed");
        }
    }

    /**
     * Synchronizes all companies from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllCompanies() {
        log.info("Syncing companies to Elasticsearch...");

        List<Company> companies = companyRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        companies.forEach(company -> {
            try {
                CompanyDocument document = CompanyDocument.fromEntity(company);
                companySearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index company: {} (ID: {})", company.getName(), company.getId(), e);
            }
        });

        log.info("✓ Synced {} companies to Elasticsearch", count.get());
    }

    /**
     * Synchronizes all locations from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllLocations() {
        log.info("Syncing locations to Elasticsearch...");

        List<Location> locations = locationRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        locations.forEach(location -> {
            try {
                LocationDocument document = LocationDocument.fromEntity(location);
                locationSearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index location: {} (ID: {})", location.getName(), location.getId(), e);
            }
        });

        log.info("✓ Synced {} locations to Elasticsearch", count.get());
    }

    /**
     * Synchronizes all zones from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllZones() {
        log.info("Syncing zones to Elasticsearch...");

        List<Zone> zones = zoneRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        zones.forEach(zone -> {
            try {
                ZoneDocument document = ZoneDocument.fromEntity(zone);
                zoneSearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index zone: {} (ID: {})", zone.getName(), zone.getId(), e);
            }
        });

        log.info("✓ Synced {} zones to Elasticsearch", count.get());
    }

    /**
     * Synchronizes all sensors from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllSensors() {
        log.info("Syncing sensors to Elasticsearch...");

        List<Sensor> sensors = sensorRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        sensors.forEach(sensor -> {
            try {
                SensorDocument document = SensorDocument.fromEntity(sensor);
                sensorSearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index sensor: {} (ID: {})", sensor.getName(), sensor.getId(), e);
            }
        });

        log.info("✓ Synced {} sensors to Elasticsearch", count.get());
    }

    /**
     * Synchronizes all reports from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllReports() {
        log.info("Syncing reports to Elasticsearch...");

        List<Report> reports = reportRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        reports.forEach(report -> {
            try {
                ReportDocument document = ReportDocument.fromEntity(report);
                reportSearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index report: {} (ID: {})", report.getName(), report.getId(), e);
            }
        });

        log.info("✓ Synced {} reports to Elasticsearch", count.get());
    }

    /**
     * Synchronizes all dashboards from MySQL to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void syncAllDashboards() {
        log.info("Syncing dashboards to Elasticsearch...");

        List<Dashboard> dashboards = dashboardRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);

        dashboards.forEach(dashboard -> {
            try {
                DashboardDocument document = DashboardDocument.fromEntity(dashboard);
                dashboardSearchRepository.save(document);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index dashboard: {} (ID: {})", dashboard.getName(), dashboard.getId(), e);
            }
        });

        log.info("✓ Synced {} dashboards to Elasticsearch", count.get());
    }

    /**
     * Synchronizes a single company to Elasticsearch.
     * Use this when a company is created or updated.
     */
    @Async
    public void syncCompany(Company company) {
        try {
            CompanyDocument document = CompanyDocument.fromEntity(company);
            companySearchRepository.save(document);
            log.debug("Synced company {} to Elasticsearch", company.getId());
        } catch (Exception e) {
            log.error("Failed to sync company {} to Elasticsearch", company.getId(), e);
        }
    }

    /**
     * Synchronizes a single location to Elasticsearch.
     */
    @Async
    public void syncLocation(Location location) {
        try {
            LocationDocument document = LocationDocument.fromEntity(location);
            locationSearchRepository.save(document);
            log.debug("Synced location {} to Elasticsearch", location.getId());
        } catch (Exception e) {
            log.error("Failed to sync location {} to Elasticsearch", location.getId(), e);
        }
    }

    /**
     * Synchronizes a single zone to Elasticsearch.
     */
    @Async
    public void syncZone(Zone zone) {
        try {
            ZoneDocument document = ZoneDocument.fromEntity(zone);
            zoneSearchRepository.save(document);
            log.debug("Synced zone {} to Elasticsearch", zone.getId());
        } catch (Exception e) {
            log.error("Failed to sync zone {} to Elasticsearch", zone.getId(), e);
        }
    }

    /**
     * Synchronizes a single sensor to Elasticsearch.
     */
    @Async
    public void syncSensor(Sensor sensor) {
        try {
            SensorDocument document = SensorDocument.fromEntity(sensor);
            sensorSearchRepository.save(document);
            log.debug("Synced sensor {} to Elasticsearch", sensor.getId());
        } catch (Exception e) {
            log.error("Failed to sync sensor {} to Elasticsearch", sensor.getId(), e);
        }
    }

    /**
     * Synchronizes a single report to Elasticsearch.
     */
    @Async
    public void syncReport(Report report) {
        try {
            ReportDocument document = ReportDocument.fromEntity(report);
            reportSearchRepository.save(document);
            log.debug("Synced report {} to Elasticsearch", report.getId());
        } catch (Exception e) {
            log.error("Failed to sync report {} to Elasticsearch", report.getId(), e);
        }
    }

    /**
     * Synchronizes a single dashboard to Elasticsearch.
     */
    @Async
    public void syncDashboard(Dashboard dashboard) {
        try {
            DashboardDocument document = DashboardDocument.fromEntity(dashboard);
            dashboardSearchRepository.save(document);
            log.debug("Synced dashboard {} to Elasticsearch", dashboard.getId());
        } catch (Exception e) {
            log.error("Failed to sync dashboard {} to Elasticsearch", dashboard.getId(), e);
        }
    }

    /**
     * Deletes a company from Elasticsearch.
     */
    @Async
    public void deleteCompany(Long companyId) {
        try {
            companySearchRepository.deleteById(companyId);
            log.debug("Deleted company {} from Elasticsearch", companyId);
        } catch (Exception e) {
            log.error("Failed to delete company {} from Elasticsearch", companyId, e);
        }
    }

    /**
     * Deletes a location from Elasticsearch.
     */
    @Async
    public void deleteLocation(Long locationId) {
        try {
            locationSearchRepository.deleteById(locationId);
            log.debug("Deleted location {} from Elasticsearch", locationId);
        } catch (Exception e) {
            log.error("Failed to delete location {} from Elasticsearch", locationId, e);
        }
    }

    /**
     * Deletes a zone from Elasticsearch.
     */
    @Async
    public void deleteZone(Long zoneId) {
        try {
            zoneSearchRepository.deleteById(zoneId);
            log.debug("Deleted zone {} from Elasticsearch", zoneId);
        } catch (Exception e) {
            log.error("Failed to delete zone {} from Elasticsearch", zoneId, e);
        }
    }

    /**
     * Deletes a sensor from Elasticsearch.
     */
    @Async
    public void deleteSensor(Long sensorId) {
        try {
            sensorSearchRepository.deleteById(sensorId);
            log.debug("Deleted sensor {} from Elasticsearch", sensorId);
        } catch (Exception e) {
            log.error("Failed to delete sensor {} from Elasticsearch", sensorId, e);
        }
    }

    /**
     * Deletes a report from Elasticsearch.
     */
    @Async
    public void deleteReport(Long reportId) {
        try {
            reportSearchRepository.deleteById(reportId);
            log.debug("Deleted report {} from Elasticsearch", reportId);
        } catch (Exception e) {
            log.error("Failed to delete report {} from Elasticsearch", reportId, e);
        }
    }

    /**
     * Deletes a dashboard from Elasticsearch.
     */
    @Async
    public void deleteDashboard(Long dashboardId) {
        try {
            dashboardSearchRepository.deleteById(dashboardId);
            log.debug("Deleted dashboard {} from Elasticsearch", dashboardId);
        } catch (Exception e) {
            log.error("Failed to delete dashboard {} from Elasticsearch", dashboardId, e);
        }
    }

    /**
     * Manually trigger a full resync. Useful for admin operations.
     */
    public void manualFullSync() {
        log.info("Manual full sync triggered by admin");
        syncAllDataOnStartup();
    }
}
