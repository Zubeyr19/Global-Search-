package com.globalsearch.service;

import com.globalsearch.entity.*;
import com.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;

    @Override
    public void run(String... args) throws Exception {
        if (companyRepository.count() == 0) {
            log.info("Initializing sample data...");
            initializeSampleData();
            log.info("Sample data initialized successfully!");
        } else {
            log.info("Data already exists, checking for missing users...");
            addMissingUsers();
        }
    }

    private void addMissingUsers() {
        // Update existing sensors with missing dates
        updateSensorDates();

        // Add missing company data for multi-tenant demo
        addMissingCompanyData();
    }

    private void updateSensorDates() {
        log.info("Checking and updating sensor dates...");
        var sensors = sensorRepository.findAll();
        int updated = 0;
        for (var sensor : sensors) {
            boolean needsUpdate = false;
            if (sensor.getInstallationDate() == null) {
                sensor.setInstallationDate(LocalDateTime.now().minusMonths(6));
                needsUpdate = true;
            }
            if (sensor.getLastReadingTime() == null && sensor.getLastReadingValue() != null) {
                sensor.setLastReadingTime(LocalDateTime.now().minusMinutes(5));
                needsUpdate = true;
            }
            if (needsUpdate) {
                sensorRepository.save(sensor);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Updated {} sensors with missing dates", updated);
        }
    }

    private void addMissingCompanyData() {
        // Check if Company 2 exists but doesn't have locations
        var companies = companyRepository.findAll();
        Company company2 = null;

        for (var company : companies) {
            if ("TENANT_MANUFACTURING".equals(company.getTenantId())) {
                company2 = company;
                break;
            }
        }

        // If Company 2 doesn't exist, create it
        if (company2 == null) {
            log.info("Adding TechManufacturing Corp (TENANT_MANUFACTURING)...");
            company2 = Company.builder()
                    .name("TechManufacturing Corp")
                    .tenantId("TENANT_MANUFACTURING")
                    .industry("Manufacturing")
                    .description("High-tech manufacturing company")
                    .contactEmail("admin@techmanufacturing.com")
                    .contactPhone("+1-555-0200")
                    .address("200 Industrial Park")
                    .city("Detroit")
                    .state("MI")
                    .country("USA")
                    .postalCode("48201")
                    .status(Company.CompanyStatus.ACTIVE)
                    .maxUsers(30)
                    .maxLocations(5)
                    .maxSensors(300)
                    .build();
            company2 = companyRepository.save(company2);
            log.info("✅ Created TechManufacturing Corp");
        }

        // Check if Company 2 has any locations
        final Long company2Id = company2.getId();
        long locationCount = locationRepository.findAll().stream()
                .filter(loc -> loc.getCompany().getId().equals(company2Id))
                .count();

        if (locationCount == 0) {
            log.info("Adding location for TechManufacturing Corp...");

            // Create location for Company 2
            Location location3 = Location.builder()
                    .name("Detroit Manufacturing Plant")
                    .type("factory")
                    .address("200 Industrial Park")
                    .city("Detroit")
                    .state("MI")
                    .country("USA")
                    .postalCode("48201")
                    .latitude(42.3314)
                    .longitude(-83.0458)
                    .description("Primary manufacturing facility")
                    .totalArea(15000.0)
                    .company(company2)
                    .status(Location.LocationStatus.ACTIVE)
                    .managerName("Mike Johnson")
                    .managerEmail("mike.johnson@techmanufacturing.com")
                    .build();
            location3 = locationRepository.save(location3);

            // Create zone for Company 2
            Zone zone3 = Zone.builder()
                    .name("Assembly Line A")
                    .type("production")
                    .description("Main assembly line")
                    .floorNumber(1)
                    .areaSize(2000.0)
                    .location(location3)
                    .status(Zone.ZoneStatus.ACTIVE)
                    .temperatureMin(18.0)
                    .temperatureMax(24.0)
                    .humidityMin(35.0)
                    .humidityMax(55.0)
                    .alertEnabled(true)
                    .build();
            zone3 = zoneRepository.save(zone3);

            log.info("✅ Added Detroit Manufacturing Plant for multi-tenant demo");
            log.info("Total companies: {}, locations: {}, zones: {}",
                    companyRepository.count(), locationRepository.count(), zoneRepository.count());
        } else {
            log.info("Multi-tenant data already complete: {} companies, {} locations",
                    companyRepository.count(), locationRepository.count());
        }
    }

    private void initializeSampleData() {
        // Create password encoder
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Create Company 1 - Warehouse Chain
        Company company1 = Company.builder()
                .name("Global Logistics Inc")
                .tenantId("TENANT_LOGISTICS")
                .industry("Logistics")
                .description("International warehouse and logistics company")
                .contactEmail("admin@globallogistics.com")
                .contactPhone("+1-555-0100")
                .address("100 Logistics Way")
                .city("Chicago")
                .state("IL")
                .country("USA")
                .postalCode("60601")
                .status(Company.CompanyStatus.ACTIVE)
                .maxUsers(50)
                .maxLocations(10)
                .maxSensors(500)
                .build();
        company1 = companyRepository.save(company1);

        // Create Company 2 - Manufacturing
        Company company2 = Company.builder()
                .name("TechManufacturing Corp")
                .tenantId("TENANT_MANUFACTURING")
                .industry("Manufacturing")
                .description("High-tech manufacturing company")
                .contactEmail("admin@techmanufacturing.com")
                .contactPhone("+1-555-0200")
                .address("200 Industrial Park")
                .city("Detroit")
                .state("MI")
                .country("USA")
                .postalCode("48201")
                .status(Company.CompanyStatus.ACTIVE)
                .maxUsers(30)
                .maxLocations(5)
                .maxSensors(300)
                .build();
        company2 = companyRepository.save(company2);

        // Create Super Admin
        User superAdmin = User.builder()
                .username("superadmin")
                .password(passwordEncoder.encode("admin123"))
                .email("superadmin@globalsearch.com")
                .firstName("Super")
                .lastName("Admin")
                .tenantId("SYSTEM")
                .companyId(null)
                .roles(Set.of(User.Role.SUPER_ADMIN))
                .enabled(true)
                .build();
        userRepository.save(superAdmin);

        // Create users for Company 1 - Admin and Regular User
        User admin1 = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("password123"))
                .email("admin@globallogistics.com")
                .firstName("John")
                .lastName("Smith")
                .tenantId(company1.getTenantId())
                .companyId(company1.getId())
                .roles(Set.of(User.Role.TENANT_ADMIN))
                .enabled(true)
                .build();
        userRepository.save(admin1);

        User regularUser = User.builder()
                .username("user")
                .password(passwordEncoder.encode("password123"))
                .email("user@globallogistics.com")
                .firstName("Bob")
                .lastName("Williams")
                .tenantId(company1.getTenantId())
                .companyId(company1.getId())
                .roles(Set.of(User.Role.OPERATOR))
                .enabled(true)
                .build();
        userRepository.save(regularUser);

        // Create locations for Company 1
        Location location1 = Location.builder()
                .name("Chicago Main Warehouse")
                .type("warehouse")
                .address("100 Logistics Way")
                .city("Chicago")
                .state("IL")
                .country("USA")
                .postalCode("60601")
                .latitude(41.8781)
                .longitude(-87.6298)
                .description("Main distribution center")
                .totalArea(10000.0)
                .company(company1)
                .status(Location.LocationStatus.ACTIVE)
                .managerName("Jane Doe")
                .managerEmail("jane.doe@globallogistics.com")
                .build();
        location1 = locationRepository.save(location1);

        Location location2 = Location.builder()
                .name("New York Distribution Center")
                .type("warehouse")
                .address("200 Distribution Ave")
                .city("New York")
                .state("NY")
                .country("USA")
                .postalCode("10001")
                .latitude(40.7128)
                .longitude(-74.0060)
                .description("East coast distribution")
                .totalArea(8000.0)
                .company(company1)
                .status(Location.LocationStatus.ACTIVE)
                .build();
        location2 = locationRepository.save(location2);

        // Create zones for Location 1
        Zone zone1 = Zone.builder()
                .name("Receiving Area")
                .type("receiving")
                .description("Inbound goods receiving")
                .floorNumber(1)
                .areaSize(1000.0)
                .location(location1)
                .status(Zone.ZoneStatus.ACTIVE)
                .temperatureMin(15.0)
                .temperatureMax(25.0)
                .humidityMin(30.0)
                .humidityMax(60.0)
                .alertEnabled(true)
                .build();
        zone1 = zoneRepository.save(zone1);

        Zone zone2 = Zone.builder()
                .name("Cold Storage")
                .type("storage")
                .description("Refrigerated storage area")
                .floorNumber(1)
                .areaSize(500.0)
                .location(location1)
                .status(Zone.ZoneStatus.ACTIVE)
                .temperatureMin(2.0)
                .temperatureMax(8.0)
                .humidityMin(40.0)
                .humidityMax(60.0)
                .alertEnabled(true)
                .build();
        zone2 = zoneRepository.save(zone2);

        // Create sensors for Zone 1
        Sensor sensor1 = Sensor.builder()
                .name("Temperature Sensor - Receiving")
                .serialNumber("TEMP-001-RCV")
                .sensorType(Sensor.SensorType.TEMPERATURE)
                .manufacturer("SensorTech")
                .model("ST-TEMP-100")
                .description("Main temperature sensor for receiving area")
                .zone(zone1)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(22.5)
                .lastReadingTime(LocalDateTime.now().minusMinutes(5))
                .unitOfMeasurement("°C")
                .readingInterval(60)
                .alertThresholdMin(15.0)
                .alertThresholdMax(25.0)
                .batteryLevel(85)
                .installationDate(LocalDateTime.now().minusMonths(6))
                .lastMaintenanceDate(LocalDateTime.now().minusMonths(1))
                .build();
        sensorRepository.save(sensor1);

        Sensor sensor2 = Sensor.builder()
                .name("Humidity Sensor - Receiving")
                .serialNumber("HUM-001-RCV")
                .sensorType(Sensor.SensorType.HUMIDITY)
                .manufacturer("SensorTech")
                .model("ST-HUM-100")
                .description("Main humidity sensor for receiving area")
                .zone(zone1)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(45.0)
                .lastReadingTime(LocalDateTime.now().minusMinutes(3))
                .unitOfMeasurement("%")
                .readingInterval(60)
                .alertThresholdMin(30.0)
                .alertThresholdMax(60.0)
                .batteryLevel(90)
                .installationDate(LocalDateTime.now().minusMonths(6))
                .build();
        sensorRepository.save(sensor2);

        Sensor sensor3 = Sensor.builder()
                .name("Door Sensor - Main Entry")
                .serialNumber("DOOR-001-RCV")
                .sensorType(Sensor.SensorType.DOOR_WINDOW)
                .manufacturer("SecureTech")
                .model("SEC-DOOR-200")
                .description("Main entry door sensor")
                .zone(zone1)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(0.0) // 0 = closed, 1 = open
                .lastReadingTime(LocalDateTime.now().minusMinutes(1))
                .unitOfMeasurement("state")
                .readingInterval(10)
                .batteryLevel(95)
                .installationDate(LocalDateTime.now().minusMonths(3))
                .build();
        sensorRepository.save(sensor3);

        // Create sensors for Cold Storage
        Sensor sensor4 = Sensor.builder()
                .name("Temperature Sensor - Cold Storage")
                .serialNumber("TEMP-001-COLD")
                .sensorType(Sensor.SensorType.TEMPERATURE)
                .manufacturer("ColdChain")
                .model("CC-TEMP-500")
                .description("Cold storage temperature monitoring")
                .zone(zone2)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(4.5)
                .lastReadingTime(LocalDateTime.now().minusMinutes(2))
                .unitOfMeasurement("°C")
                .readingInterval(30)
                .alertThresholdMin(2.0)
                .alertThresholdMax(8.0)
                .batteryLevel(75)
                .installationDate(LocalDateTime.now().minusMonths(8))
                .lastMaintenanceDate(LocalDateTime.now().minusWeeks(2))
                .build();
        sensorRepository.save(sensor4);

        // Create locations for Company 2 (Manufacturing)
        Location location3 = Location.builder()
                .name("Detroit Manufacturing Plant")
                .type("factory")
                .address("200 Industrial Park")
                .city("Detroit")
                .state("MI")
                .country("USA")
                .postalCode("48201")
                .latitude(42.3314)
                .longitude(-83.0458)
                .description("Primary manufacturing facility")
                .totalArea(15000.0)
                .company(company2)
                .status(Location.LocationStatus.ACTIVE)
                .managerName("Mike Johnson")
                .managerEmail("mike.johnson@techmanufacturing.com")
                .build();
        location3 = locationRepository.save(location3);

        // Create zones for Location 3
        Zone zone3 = Zone.builder()
                .name("Assembly Line A")
                .type("production")
                .description("Main assembly line")
                .floorNumber(1)
                .areaSize(2000.0)
                .location(location3)
                .status(Zone.ZoneStatus.ACTIVE)
                .temperatureMin(18.0)
                .temperatureMax(24.0)
                .humidityMin(35.0)
                .humidityMax(55.0)
                .alertEnabled(true)
                .build();
        zone3 = zoneRepository.save(zone3);

        // Create sensors for Manufacturing
        Sensor sensor5 = Sensor.builder()
                .name("Pressure Sensor - Assembly Line A")
                .serialNumber("PRESS-001-ASMA")
                .sensorType(Sensor.SensorType.PRESSURE)
                .manufacturer("IndustrialSense")
                .model("IS-PRESS-300")
                .description("Monitors air pressure in assembly line")
                .zone(zone3)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(101.3)
                .lastReadingTime(LocalDateTime.now().minusMinutes(1))
                .unitOfMeasurement("kPa")
                .readingInterval(30)
                .alertThresholdMin(95.0)
                .alertThresholdMax(105.0)
                .batteryLevel(88)
                .installationDate(LocalDateTime.now().minusMonths(12))
                .lastMaintenanceDate(LocalDateTime.now().minusMonths(3))
                .build();
        sensorRepository.save(sensor5);

        Sensor sensor6 = Sensor.builder()
                .name("Vibration Sensor - Assembly Line A")
                .serialNumber("VIB-001-ASMA")
                .sensorType(Sensor.SensorType.VIBRATION)
                .manufacturer("IndustrialSense")
                .model("IS-VIB-200")
                .description("Detects abnormal vibrations")
                .zone(zone3)
                .status(Sensor.SensorStatus.ACTIVE)
                .lastReadingValue(0.5)
                .lastReadingTime(LocalDateTime.now().minusSeconds(45))
                .unitOfMeasurement("mm/s")
                .readingInterval(15)
                .alertThresholdMin(0.0)
                .alertThresholdMax(2.0)
                .batteryLevel(92)
                .installationDate(LocalDateTime.now().minusMonths(10))
                .build();
        sensorRepository.save(sensor6);

        // Log summary
        log.info("Created {} companies", companyRepository.count());
        log.info("Created {} users", userRepository.count());
        log.info("Created {} locations", locationRepository.count());
        log.info("Created {} zones", zoneRepository.count());
        log.info("Created {} sensors", sensorRepository.count());

        log.info("=== Sample Login Credentials ===");
        log.info("Super Admin: username=superadmin, password=admin123 (Sees ALL data)");
        log.info("Company Admin (TENANT_LOGISTICS): username=admin, password=password123");
        log.info("Regular User (TENANT_LOGISTICS): username=user, password=password123");
    }
}