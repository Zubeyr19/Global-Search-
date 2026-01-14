package com.globalsearch.controller;

import com.globalsearch.entity.*;
import com.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/seed")
@RequiredArgsConstructor
@Slf4j
public class DataSeedController {

    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final DashboardRepository dashboardRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    private static final String[] INDUSTRIES = {"Manufacturing", "Healthcare", "Retail", "Logistics", "Energy"};
    private static final String[] CITIES = {"Copenhagen", "Aarhus", "Odense", "Aalborg", "Esbjerg"};
    private static final String[] COUNTRIES = {"Denmark", "Germany", "Sweden", "Norway"};
    private static final String[] LOCATION_TYPES = {"warehouse", "factory", "office", "store"};
    private static final String[] ZONE_TYPES = {"storage", "production", "office", "parking"};
    private static final String[] MANUFACTURERS = {"Siemens", "Honeywell", "Schneider", "ABB", "Bosch"};

    @PostMapping("/production-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedProductionData() {
        log.info("🚀 Starting production-scale data seeding via API...");

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if data already exists
            long existingCompanies = companyRepository.count();
            if (existingCompanies > 10) {
                result.put("status", "skipped");
                result.put("message", "Database already contains " + existingCompanies + " companies");
                return ResponseEntity.ok(result);
            }

            // Seed data
            List<Company> companies = seedCompanies(100);
            seedLocationsAndZones(companies, 50, 2);
            seedSensors(3);
            seedDashboards(companies, 20);
            seedReports(companies, 30);
            seedUsers(companies, 5);

            long duration = System.currentTimeMillis() - startTime;

            result.put("status", "success");
            result.put("duration_seconds", duration / 1000);
            result.put("companies", companyRepository.count());
            result.put("locations", locationRepository.count());
            result.put("zones", zoneRepository.count());
            result.put("sensors", sensorRepository.count());
            result.put("dashboards", dashboardRepository.count());
            result.put("reports", reportRepository.count());
            result.put("users", userRepository.count());

            long total = companyRepository.count() + locationRepository.count() +
                         zoneRepository.count() + sensorRepository.count() +
                         dashboardRepository.count() + reportRepository.count() +
                         userRepository.count();
            result.put("total_entities", total);

            log.info("✅ Data seeding completed in {} seconds", duration / 1000);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Data seeding failed", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("companies", companyRepository.count());
        stats.put("locations", locationRepository.count());
        stats.put("zones", zoneRepository.count());
        stats.put("sensors", sensorRepository.count());
        stats.put("dashboards", dashboardRepository.count());
        stats.put("reports", reportRepository.count());
        stats.put("users", userRepository.count());

        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        stats.put("total", total);

        return ResponseEntity.ok(stats);
    }

    private List<Company> seedCompanies(int count) {
        log.info("Seeding {} companies...", count);
        List<Company> companies = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            Company company = Company.builder()
                    .name("Company_" + i)
                    .tenantId("TENANT_" + i)
                    .industry(INDUSTRIES[random.nextInt(INDUSTRIES.length)])
                    .description("Production test company " + i)
                    .contactEmail("contact@company" + i + ".com")
                    .contactPhone("+45 " + (20000000 + i))
                    .address(random.nextInt(100) + " Main Street")
                    .city(CITIES[random.nextInt(CITIES.length)])
                    .country(COUNTRIES[random.nextInt(COUNTRIES.length)])
                    .postalCode(String.format("%04d", 1000 + i))
                    .status(Company.CompanyStatus.ACTIVE)
                    .maxUsers(50)
                    .maxLocations(100)
                    .maxSensors(2000)
                    .build();
            companies.add(company);
        }

        List<Company> saved = companyRepository.saveAll(companies);
        companyRepository.flush();
        log.info("✓ Created {} companies", saved.size());
        return saved;
    }

    private void seedLocationsAndZones(List<Company> companies, int locationsPerCompany, int zonesPerLocation) {
        log.info("Seeding locations and zones...");

        List<Location> allLocations = new ArrayList<>();

        for (Company company : companies) {
            for (int i = 1; i <= locationsPerCompany; i++) {
                Location location = Location.builder()
                        .name("Location_" + company.getId() + "_" + i)
                        .type(LOCATION_TYPES[random.nextInt(LOCATION_TYPES.length)])
                        .address(random.nextInt(500) + " Industrial Road")
                        .city(CITIES[random.nextInt(CITIES.length)])
                        .country(company.getCountry())
                        .postalCode(String.format("%04d", 2000 + random.nextInt(8000)))
                        .latitude(55.0 + random.nextDouble() * 2.0)
                        .longitude(8.0 + random.nextDouble() * 6.0)
                        .totalArea(500.0 + random.nextDouble() * 4500.0)
                        .status(Location.LocationStatus.ACTIVE)
                        .company(company)
                        .build();
                allLocations.add(location);
            }

            if (allLocations.size() >= 500) {
                locationRepository.saveAll(allLocations);
                locationRepository.flush();
                log.info("Progress: {} locations saved", locationRepository.count());
                allLocations.clear();
            }
        }

        if (!allLocations.isEmpty()) {
            locationRepository.saveAll(allLocations);
            locationRepository.flush();
        }

        log.info("✓ Created {} locations", locationRepository.count());

        // Now create zones
        List<Location> locations = locationRepository.findAll();
        List<Zone> allZones = new ArrayList<>();

        for (Location location : locations) {
            for (int i = 1; i <= zonesPerLocation; i++) {
                Zone zone = Zone.builder()
                        .name("Zone_" + location.getId() + "_" + i)
                        .type(ZONE_TYPES[random.nextInt(ZONE_TYPES.length)])
                        .description("Zone " + i)
                        .floorNumber(random.nextInt(5))
                        .areaSize(50.0 + random.nextDouble() * 450.0)
                        .location(location)
                        .status(Zone.ZoneStatus.ACTIVE)
                        .temperatureMin(18.0)
                        .temperatureMax(26.0)
                        .humidityMin(35.0)
                        .humidityMax(65.0)
                        .alertEnabled(true)
                        .build();
                allZones.add(zone);
            }

            if (allZones.size() >= 1000) {
                zoneRepository.saveAll(allZones);
                zoneRepository.flush();
                log.info("Progress: {} zones saved", zoneRepository.count());
                allZones.clear();
            }
        }

        if (!allZones.isEmpty()) {
            zoneRepository.saveAll(allZones);
            zoneRepository.flush();
        }

        log.info("✓ Created {} zones", zoneRepository.count());
    }

    private void seedSensors(int sensorsPerZone) {
        log.info("Seeding sensors...");

        List<Zone> zones = zoneRepository.findAll();
        List<Sensor> allSensors = new ArrayList<>();
        Sensor.SensorType[] types = Sensor.SensorType.values();

        for (Zone zone : zones) {
            for (int i = 1; i <= sensorsPerZone; i++) {
                Sensor.SensorType type = types[random.nextInt(types.length)];

                Sensor sensor = Sensor.builder()
                        .name("Sensor_" + zone.getId() + "_" + i)
                        .serialNumber("SN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .sensorType(type)
                        .manufacturer(MANUFACTURERS[random.nextInt(MANUFACTURERS.length)])
                        .model("Model-" + (1000 + random.nextInt(9000)))
                        .description(type + " sensor")
                        .zone(zone)
                        .status(Sensor.SensorStatus.ACTIVE)
                        .lastReadingTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .lastReadingValue(20.0 + random.nextDouble() * 10.0)
                        .unitOfMeasurement(getUnit(type))
                        .readingInterval(120)
                        .alertThresholdMin(15.0)
                        .alertThresholdMax(30.0)
                        .batteryLevel(80 + random.nextInt(20))
                        .installationDate(LocalDateTime.now().minusDays(random.nextInt(365)))
                        .build();
                allSensors.add(sensor);
            }

            if (allSensors.size() >= 1000) {
                sensorRepository.saveAll(allSensors);
                sensorRepository.flush();
                log.info("Progress: {} sensors saved", sensorRepository.count());
                allSensors.clear();
            }
        }

        if (!allSensors.isEmpty()) {
            sensorRepository.saveAll(allSensors);
            sensorRepository.flush();
        }

        log.info("✓ Created {} sensors", sensorRepository.count());
    }

    private void seedDashboards(List<Company> companies, int perCompany) {
        log.info("Seeding dashboards...");

        List<Dashboard> dashboards = new ArrayList<>();
        Dashboard.DashboardType[] types = Dashboard.DashboardType.values();

        for (Company company : companies) {
            for (int i = 1; i <= perCompany; i++) {
                Dashboard dashboard = Dashboard.builder()
                        .name("Dashboard_" + company.getId() + "_" + i)
                        .tenantId(company.getTenantId())
                        .ownerId(1L)
                        .ownerName("User_1")
                        .description("Dashboard " + i)
                        .dashboardType(types[random.nextInt(types.length)])
                        .isDefault(i == 1)
                        .isShared(random.nextBoolean())
                        .refreshInterval(60)
                        .tags("monitoring,production")
                        .accessCount(random.nextInt(500))
                        .build();
                dashboards.add(dashboard);
            }
        }

        dashboardRepository.saveAll(dashboards);
        dashboardRepository.flush();
        log.info("✓ Created {} dashboards", dashboardRepository.count());
    }

    private void seedReports(List<Company> companies, int perCompany) {
        log.info("Seeding reports...");

        List<Report> reports = new ArrayList<>();
        String[] types = {"DAILY", "WEEKLY", "MONTHLY", "CUSTOM"};

        for (Company company : companies) {
            for (int i = 1; i <= perCompany; i++) {
                Report report = Report.builder()
                        .name("Report_" + company.getId() + "_" + i)
                        .tenantId(company.getTenantId())
                        .reportType(types[random.nextInt(types.length)])
                        .description("Report " + i)
                        .filePath("/reports/report_" + i + ".pdf")
                        .fileSize((long) (1024 * 1000))
                        .mimeType("application/pdf")
                        .createdBy(1L)
                        .tags("production,analysis")
                        .isPublic(false)
                        .executionTimeMs(5000L)
                        .status(Report.ReportStatus.COMPLETED)
                        .build();
                reports.add(report);
            }
        }

        reportRepository.saveAll(reports);
        reportRepository.flush();
        log.info("✓ Created {} reports", reportRepository.count());
    }

    private void seedUsers(List<Company> companies, int perCompany) {
        log.info("Seeding users...");

        List<User> users = new ArrayList<>();

        for (Company company : companies) {
            for (int i = 1; i <= perCompany; i++) {
                Set<User.Role> roles = new HashSet<>();
                roles.add(User.Role.VIEWER);

                User user = User.builder()
                        .username("user_" + company.getId() + "_" + i)
                        .password(passwordEncoder.encode("password123"))
                        .email("user" + i + "@company" + company.getId() + ".com")
                        .firstName("First" + i)
                        .lastName("Last" + i)
                        .tenantId(company.getTenantId())
                        .companyId(company.getId())
                        .roles(roles)
                        .enabled(true)
                        .build();
                users.add(user);
            }
        }

        userRepository.saveAll(users);
        userRepository.flush();
        log.info("✓ Created {} users", userRepository.count());
    }

    private String getUnit(Sensor.SensorType type) {
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
}
