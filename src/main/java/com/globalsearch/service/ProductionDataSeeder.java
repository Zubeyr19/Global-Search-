package com.globalsearch.service;

import com.globalsearch.entity.*;
import com.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Production-scale data seeder for load testing and demonstration
 * Generates ~50,600 entities across all types
 */
//@Component  // Disabled - use REST API endpoint instead: POST /api/admin/seed/production-data
@RequiredArgsConstructor
@Slf4j
public class ProductionDataSeeder implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final DashboardRepository dashboardRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42); // Fixed seed for reproducibility

    // Configuration
    private static final int NUM_COMPANIES = 100;
    private static final int LOCATIONS_PER_COMPANY = 50;
    private static final int ZONES_PER_LOCATION = 2;
    private static final int SENSORS_PER_ZONE = 3;
    private static final int DASHBOARDS_PER_COMPANY = 20;
    private static final int REPORTS_PER_COMPANY = 30;
    private static final int USERS_PER_COMPANY = 5;

    // Data arrays for realistic generation
    private static final String[] INDUSTRIES = {"Manufacturing", "Healthcare", "Retail", "Logistics", "Energy",
        "Agriculture", "Technology", "Food & Beverage", "Pharmaceutical", "Automotive"};

    private static final String[] CITIES = {"Copenhagen", "Aarhus", "Odense", "Aalborg", "Esbjerg",
        "Randers", "Kolding", "Horsens", "Vejle", "Roskilde", "Herning", "Silkeborg"};

    private static final String[] COUNTRIES = {"Denmark", "Germany", "Sweden", "Norway", "Netherlands"};

    private static final String[] LOCATION_TYPES = {"warehouse", "factory", "office", "store", "distribution_center"};

    private static final String[] ZONE_TYPES = {"storage", "production", "office", "parking", "loading_dock"};

    private static final String[] SENSOR_MANUFACTURERS = {"Siemens", "Honeywell", "Schneider Electric",
        "ABB", "Bosch", "Texas Instruments"};

    @Override
    @Transactional
    public void run(String... args) {
        // Check if data already exists
        long existingCompanies = companyRepository.count();
        if (existingCompanies > 10) {
            log.info("Database already contains {} companies. Skipping data seeding.", existingCompanies);
            log.info("To re-seed, delete existing data first");
            return;
        }

        log.info("🚀 Starting production-scale data seeding...");
        log.info("Target: ~50,600 entities");

        long startTime = System.currentTimeMillis();

        try {
            List<Company> companies = seedCompanies();
            seedLocationsAndZones(companies);
            seedSensors();
            seedDashboards(companies);
            seedReports(companies);
            seedUsers(companies);

            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ Data seeding completed in {} seconds", duration / 1000);
            printStatistics();

        } catch (Exception e) {
            log.error("❌ Data seeding failed", e);
            throw new RuntimeException("Data seeding failed", e);
        }
    }

    private List<Company> seedCompanies() {
        log.info("Seeding {} companies...", NUM_COMPANIES);
        List<Company> companies = new ArrayList<>();

        for (int i = 1; i <= NUM_COMPANIES; i++) {
            Company company = Company.builder()
                    .name("Company_" + i)
                    .tenantId("TENANT_" + i)
                    .industry(INDUSTRIES[random.nextInt(INDUSTRIES.length)])
                    .description("Production-scale test company " + i)
                    .contactEmail("contact@company" + i + ".com")
                    .contactPhone("+45 " + (20000000 + i))
                    .address(random.nextInt(100) + " Main Street")
                    .city(CITIES[random.nextInt(CITIES.length)])
                    .country(COUNTRIES[random.nextInt(COUNTRIES.length)])
                    .postalCode(String.format("%04d", 1000 + i))
                    .status(Company.CompanyStatus.ACTIVE)
                    .maxUsers(10 + random.nextInt(40))
                    .maxLocations(50 + random.nextInt(50))
                    .maxSensors(1000 + random.nextInt(1000))
                    .build();

            companies.add(company);
        }

        List<Company> saved = companyRepository.saveAll(companies);
        companyRepository.flush();
        log.info("✓ Created {} companies", saved.size());
        return saved;
    }

    private void seedLocationsAndZones(List<Company> companies) {
        log.info("Seeding {} locations...", NUM_COMPANIES * LOCATIONS_PER_COMPANY);

        int batchSize = 500;
        List<Location> locationBatch = new ArrayList<>(batchSize);
        int totalLocations = 0;

        for (Company company : companies) {
            for (int i = 1; i <= LOCATIONS_PER_COMPANY; i++) {
                Location location = Location.builder()
                        .name("Location_" + company.getName() + "_" + i)
                        .type(LOCATION_TYPES[random.nextInt(LOCATION_TYPES.length)])
                        .address(random.nextInt(500) + " Industrial Road")
                        .city(CITIES[random.nextInt(CITIES.length)])
                        .country(company.getCountry())
                        .postalCode(String.format("%04d", 2000 + random.nextInt(9000)))
                        .latitude(55.0 + random.nextDouble() * 2.0)
                        .longitude(8.0 + random.nextDouble() * 6.0)
                        .description("Production location for " + company.getName())
                        .totalArea(500.0 + random.nextDouble() * 4500.0)
                        .status(Location.LocationStatus.ACTIVE)
                        .company(company)
                        .build();

                locationBatch.add(location);
                totalLocations++;

                if (locationBatch.size() >= batchSize) {
                    locationRepository.saveAll(locationBatch);
                    locationRepository.flush();
                    locationBatch.clear();
                    log.info("Progress: {} locations created", totalLocations);
                }
            }
        }

        if (!locationBatch.isEmpty()) {
            locationRepository.saveAll(locationBatch);
            locationRepository.flush();
        }

        log.info("✓ Created {} locations", totalLocations);

        // Now seed zones for all locations
        log.info("Seeding zones...");
        List<Location> allLocations = locationRepository.findAll();
        List<Zone> zoneBatch = new ArrayList<>(batchSize);
        int totalZones = 0;

        for (Location location : allLocations) {
            for (int i = 1; i <= ZONES_PER_LOCATION; i++) {
                Zone zone = Zone.builder()
                        .name("Zone_" + location.getName() + "_" + i)
                        .type(ZONE_TYPES[random.nextInt(ZONE_TYPES.length)])
                        .description("Zone " + i + " in " + location.getName())
                        .floorNumber(random.nextInt(5))
                        .areaSize(50.0 + random.nextDouble() * 450.0)
                        .location(location)
                        .status(Zone.ZoneStatus.ACTIVE)
                        .temperatureMin(15.0 + random.nextDouble() * 5.0)
                        .temperatureMax(25.0 + random.nextDouble() * 5.0)
                        .humidityMin(30.0 + random.nextDouble() * 10.0)
                        .humidityMax(60.0 + random.nextDouble() * 20.0)
                        .alertEnabled(true)
                        .build();

                zoneBatch.add(zone);
                totalZones++;

                if (zoneBatch.size() >= batchSize) {
                    zoneRepository.saveAll(zoneBatch);
                    zoneRepository.flush();
                    zoneBatch.clear();
                    log.info("Progress: {} zones created", totalZones);
                }
            }
        }

        if (!zoneBatch.isEmpty()) {
            zoneRepository.saveAll(zoneBatch);
            zoneRepository.flush();
        }

        log.info("✓ Created {} zones", totalZones);
    }

    private void seedSensors() {
        log.info("Seeding sensors...");

        List<Zone> allZones = zoneRepository.findAll();
        int batchSize = 500;
        List<Sensor> sensorBatch = new ArrayList<>(batchSize);
        int totalSensors = 0;

        Sensor.SensorType[] sensorTypes = Sensor.SensorType.values();

        for (Zone zone : allZones) {
            for (int i = 1; i <= SENSORS_PER_ZONE; i++) {
                Sensor.SensorType type = sensorTypes[random.nextInt(sensorTypes.length)];

                Sensor sensor = Sensor.builder()
                        .name("Sensor_" + zone.getName() + "_" + i)
                        .serialNumber("SN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .sensorType(type)
                        .manufacturer(SENSOR_MANUFACTURERS[random.nextInt(SENSOR_MANUFACTURERS.length)])
                        .model("Model-" + (1000 + random.nextInt(9000)))
                        .description(type + " sensor in " + zone.getName())
                        .zone(zone)
                        .status(Sensor.SensorStatus.ACTIVE)
                        .lastReadingTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .lastReadingValue(20.0 + random.nextDouble() * 10.0)
                        .unitOfMeasurement(getUnitForType(type))
                        .readingInterval(60 + random.nextInt(240))
                        .alertThresholdMin(15.0)
                        .alertThresholdMax(30.0)
                        .batteryLevel(70 + random.nextInt(30))
                        .installationDate(LocalDateTime.now().minusDays(random.nextInt(365)))
                        .build();

                sensorBatch.add(sensor);
                totalSensors++;

                if (sensorBatch.size() >= batchSize) {
                    sensorRepository.saveAll(sensorBatch);
                    sensorRepository.flush();
                    sensorBatch.clear();
                    log.info("Progress: {} sensors created", totalSensors);
                }
            }
        }

        if (!sensorBatch.isEmpty()) {
            sensorRepository.saveAll(sensorBatch);
            sensorRepository.flush();
        }

        log.info("✓ Created {} sensors", totalSensors);
    }

    private void seedDashboards(List<Company> companies) {
        log.info("Seeding dashboards...");

        List<Dashboard> dashboardBatch = new ArrayList<>();
        int totalDashboards = 0;

        Dashboard.DashboardType[] types = Dashboard.DashboardType.values();

        for (Company company : companies) {
            for (int i = 1; i <= DASHBOARDS_PER_COMPANY; i++) {
                Dashboard dashboard = Dashboard.builder()
                        .name("Dashboard_" + company.getName() + "_" + i)
                        .tenantId(company.getTenantId())
                        .ownerId((long) (random.nextInt(1000) + 1))
                        .ownerName("User_" + random.nextInt(100))
                        .description("Dashboard " + i + " for " + company.getName())
                        .dashboardType(types[random.nextInt(types.length)])
                        .isDefault(i == 1)
                        .isShared(random.nextBoolean())
                        .isFavorite(random.nextBoolean())
                        .refreshInterval(30 + random.nextInt(270))
                        .tags("tag1,tag2,monitoring")
                        .accessCount(random.nextInt(1000))
                        .configuration("{\"widgets\": []}")
                        .layout("{\"columns\": 3}")
                        .build();

                dashboardBatch.add(dashboard);
                totalDashboards++;
            }
        }

        dashboardRepository.saveAll(dashboardBatch);
        dashboardRepository.flush();
        log.info("✓ Created {} dashboards", totalDashboards);
    }

    private void seedReports(List<Company> companies) {
        log.info("Seeding reports...");

        List<Report> reportBatch = new ArrayList<>();
        int totalReports = 0;

        String[] reportTypes = {"DAILY", "WEEKLY", "MONTHLY", "CUSTOM", "SENSOR_HEALTH", "ALERT_SUMMARY"};
        Report.ReportStatus[] statuses = Report.ReportStatus.values();

        for (Company company : companies) {
            for (int i = 1; i <= REPORTS_PER_COMPANY; i++) {
                Report report = Report.builder()
                        .name("Report_" + company.getName() + "_" + i)
                        .tenantId(company.getTenantId())
                        .reportType(reportTypes[random.nextInt(reportTypes.length)])
                        .description("Report " + i + " for " + company.getName())
                        .filePath("/reports/" + company.getTenantId() + "/report_" + i + ".pdf")
                        .fileSize((long) (1024 * (500 + random.nextInt(9500))))
                        .mimeType("application/pdf")
                        .createdBy((long) (random.nextInt(1000) + 1))
                        .tags("production,monitoring,analysis")
                        .isPublic(random.nextBoolean())
                        .parameters("{\"period\": \"monthly\"}")
                        .executionTimeMs((long) (1000 + random.nextInt(9000)))
                        .status(statuses[random.nextInt(statuses.length)])
                        .build();

                reportBatch.add(report);
                totalReports++;
            }
        }

        reportRepository.saveAll(reportBatch);
        reportRepository.flush();
        log.info("✓ Created {} reports", totalReports);
    }

    private void seedUsers(List<Company> companies) {
        log.info("Seeding users...");

        List<User> userBatch = new ArrayList<>();
        int totalUsers = 0;

        User.Role[] roles = User.Role.values();

        for (Company company : companies) {
            for (int i = 1; i <= USERS_PER_COMPANY; i++) {
                Set<User.Role> userRoles = new HashSet<>();
                userRoles.add(roles[random.nextInt(roles.length)]);

                User user = User.builder()
                        .username("user_" + company.getName().toLowerCase() + "_" + i)
                        .password(passwordEncoder.encode("password123"))
                        .email("user" + i + "@" + company.getName().toLowerCase() + ".com")
                        .firstName("FirstName" + i)
                        .lastName("LastName" + i)
                        .tenantId(company.getTenantId())
                        .companyId(company.getId())
                        .roles(userRoles)
                        .enabled(true)
                        .lastLogin(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .build();

                userBatch.add(user);
                totalUsers++;
            }
        }

        userRepository.saveAll(userBatch);
        userRepository.flush();
        log.info("✓ Created {} users", totalUsers);
    }

    private String getUnitForType(Sensor.SensorType type) {
        return switch (type) {
            case TEMPERATURE -> "°C";
            case HUMIDITY -> "%";
            case PRESSURE -> "Pa";
            case LIGHT -> "lux";
            case POWER_METER -> "kWh";
            case AIR_QUALITY -> "ppm";
            default -> "units";
        };
    }

    private void printStatistics() {
        log.info("=".repeat(60));
        log.info("📊 DATABASE STATISTICS");
        log.info("=".repeat(60));
        log.info("Companies:   {}", companyRepository.count());
        log.info("Locations:   {}", locationRepository.count());
        log.info("Zones:       {}", zoneRepository.count());
        log.info("Sensors:     {}", sensorRepository.count());
        log.info("Dashboards:  {}", dashboardRepository.count());
        log.info("Reports:     {}", reportRepository.count());
        log.info("Users:       {}", userRepository.count());
        log.info("-".repeat(60));

        long total = companyRepository.count() + locationRepository.count() + zoneRepository.count() +
                     sensorRepository.count() + dashboardRepository.count() + reportRepository.count() +
                     userRepository.count();

        log.info("TOTAL:       {}", total);
        log.info("=".repeat(60));
    }
}
