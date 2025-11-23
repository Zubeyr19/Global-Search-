package com.globalsearch.service;

import com.globalsearch.entity.*;
import com.globalsearch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // No missing users to add - all users are created in initializeSampleData
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
                .unitOfMeasurement("°C")
                .readingInterval(60)
                .alertThresholdMin(15.0)
                .alertThresholdMax(25.0)
                .batteryLevel(85)
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
                .unitOfMeasurement("%")
                .readingInterval(60)
                .alertThresholdMin(30.0)
                .alertThresholdMax(60.0)
                .batteryLevel(90)
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
                .unitOfMeasurement("state")
                .readingInterval(10)
                .batteryLevel(95)
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
                .unitOfMeasurement("°C")
                .readingInterval(30)
                .alertThresholdMin(2.0)
                .alertThresholdMax(8.0)
                .batteryLevel(75)
                .build();
        sensorRepository.save(sensor4);

        // Log summary
        log.info("Created {} companies", companyRepository.count());
        log.info("Created {} users", userRepository.count());
        log.info("Created {} locations", locationRepository.count());
        log.info("Created {} zones", zoneRepository.count());
        log.info("Created {} sensors", sensorRepository.count());

        log.info("=== Sample Login Credentials ===");
        log.info("Super Admin: username=superadmin, password=admin123");
        log.info("Company Admin: username=admin_logistics, password=password123");
        log.info("Manager: username=manager_chicago, password=password123");
    }
}