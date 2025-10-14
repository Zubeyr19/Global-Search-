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
import com.globalsearch.dto.WebSocketMessage;
import com.globalsearch.service.AuditLogService;
import com.globalsearch.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final CompanySearchRepository companySearchRepository;
    private final LocationSearchRepository locationSearchRepository;
    private final ZoneSearchRepository zoneSearchRepository;
    private final SensorSearchRepository sensorSearchRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * Global search with document-level security enforcement
     * Users only see data they have access to based on tenantId and roles
     */
    public GlobalSearchResponse globalSearch(GlobalSearchRequest request, User currentUser, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();

        log.debug("Global search for user: {}, tenant: {}, query: {}",
                currentUser.getUsername(), currentUser.getTenantId(), request.getQuery());

        // Create pageable
        Pageable pageable = createPageable(request);

        // Collect all results
        List<GlobalSearchResponse.SearchResultItem> allResults = new ArrayList<>();

        // Search entities based on user permissions
        if (shouldSearchEntity(request, "companies")) {
            allResults.addAll(searchCompanies(request, currentUser));
        }

        if (shouldSearchEntity(request, "locations")) {
            allResults.addAll(searchLocations(request, currentUser));
        }

        if (shouldSearchEntity(request, "zones")) {
            allResults.addAll(searchZones(request, currentUser));
        }

        if (shouldSearchEntity(request, "sensors")) {
            allResults.addAll(searchSensors(request, currentUser));
        }

        // Sort by relevance or specified field
        allResults.sort(Comparator.comparing(GlobalSearchResponse.SearchResultItem::getRelevanceScore).reversed());

        // Apply pagination
        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), allResults.size());
        List<GlobalSearchResponse.SearchResultItem> paginatedResults =
                allResults.subList(Math.min(start, allResults.size()), end);

        long duration = System.currentTimeMillis() - startTime;

        // Log search event for audit trail
        auditLogService.logSearchEvent(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getTenantId(),
                request.getQuery(),
                allResults.size(),
                httpRequest
        );

        // Send real-time notification to user about search completion
        GlobalSearchResponse response = GlobalSearchResponse.builder()
                .results(paginatedResults)
                .totalResults((long) allResults.size())
                .currentPage(request.getPage())
                .totalPages((int) Math.ceil((double) allResults.size() / request.getSize()))
                .pageSize(request.getSize())
                .searchDurationMs(duration)
                .build();

        // Notify user about search results
        WebSocketMessage notification = WebSocketMessage.searchResult(
                currentUser.getTenantId(),
                String.format("Search completed: Found %d results for '%s' in %dms",
                        allResults.size(), request.getQuery(), duration),
                Map.of("totalResults", allResults.size(), "duration", duration)
        );
        notificationService.notifyUser(currentUser.getId(), notification);

        return response;
    }

    /**
     * Admin search - can search across all tenants
     */
    public GlobalSearchResponse adminGlobalSearch(GlobalSearchRequest request, User admin, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();

        log.debug("Admin global search by: {}, query: {}", admin.getUsername(), request.getQuery());

        // Verify admin role
        if (!admin.isAdmin()) {
            throw new SecurityException("User does not have admin privileges");
        }

        // Admin can search without tenant restriction
        List<GlobalSearchResponse.SearchResultItem> allResults = new ArrayList<>();

        if (shouldSearchEntity(request, "companies")) {
            allResults.addAll(adminSearchCompanies(request));
        }

        if (shouldSearchEntity(request, "locations")) {
            allResults.addAll(adminSearchLocations(request));
        }

        if (shouldSearchEntity(request, "zones")) {
            allResults.addAll(adminSearchZones(request));
        }

        if (shouldSearchEntity(request, "sensors")) {
            allResults.addAll(adminSearchSensors(request));
        }

        // Sort and paginate
        allResults.sort(Comparator.comparing(GlobalSearchResponse.SearchResultItem::getRelevanceScore).reversed());

        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), allResults.size());
        List<GlobalSearchResponse.SearchResultItem> paginatedResults =
                allResults.subList(Math.min(start, allResults.size()), end);

        long duration = System.currentTimeMillis() - startTime;

        // Log admin search event for audit trail
        auditLogService.logSearchEvent(
                admin.getId(),
                admin.getUsername(),
                "ADMIN_CROSS_TENANT",
                request.getQuery(),
                allResults.size(),
                httpRequest
        );

        return GlobalSearchResponse.builder()
                .results(paginatedResults)
                .totalResults((long) allResults.size())
                .currentPage(request.getPage())
                .totalPages((int) Math.ceil((double) allResults.size() / request.getSize()))
                .pageSize(request.getSize())
                .searchDurationMs(duration)
                .build();
    }

    // ==================== TENANT-RESTRICTED SEARCH METHODS ====================

    private List<GlobalSearchResponse.SearchResultItem> searchCompanies(GlobalSearchRequest request, User user) {
        List<CompanyDocument> companies;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            companies = companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());
        } else {
            companies = companySearchRepository.findByTenantId(user.getTenantId());
        }

        return companies.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchLocations(GlobalSearchRequest request, User user) {
        List<LocationDocument> locations;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            locations = locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());
        } else {
            locations = locationSearchRepository.findByTenantId(user.getTenantId());
        }

        // Apply additional filters
        if (request.getCity() != null) {
            locations = locations.stream()
                    .filter(l -> request.getCity().equalsIgnoreCase(l.getCity()))
                    .collect(Collectors.toList());
        }

        if (request.getCountry() != null) {
            locations = locations.stream()
                    .filter(l -> request.getCountry().equalsIgnoreCase(l.getCountry()))
                    .collect(Collectors.toList());
        }

        return locations.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchZones(GlobalSearchRequest request, User user) {
        List<ZoneDocument> zones;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            zones = zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());
        } else {
            zones = zoneSearchRepository.findByTenantId(user.getTenantId());
        }

        // Filter by location
        if (request.getLocationId() != null) {
            zones = zones.stream()
                    .filter(z -> request.getLocationId().equals(z.getLocationId()))
                    .collect(Collectors.toList());
        }

        return zones.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchSensors(GlobalSearchRequest request, User user) {
        List<SensorDocument> sensors;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            sensors = sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());
        } else {
            sensors = sensorSearchRepository.findByTenantId(user.getTenantId());
        }

        // Apply filters
        if (request.getSensorType() != null) {
            sensors = sensors.stream()
                    .filter(s -> request.getSensorType().equalsIgnoreCase(s.getSensorType()))
                    .collect(Collectors.toList());
        }

        if (request.getZoneId() != null) {
            sensors = sensors.stream()
                    .filter(s -> request.getZoneId().equals(s.getZoneId()))
                    .collect(Collectors.toList());
        }

        return sensors.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    // ==================== ADMIN SEARCH METHODS (NO TENANT RESTRICTION) ====================

    private List<GlobalSearchResponse.SearchResultItem> adminSearchCompanies(GlobalSearchRequest request) {
        List<CompanyDocument> companies;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            companies = companySearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            companies = (List<CompanyDocument>) companySearchRepository.findAll();
        }

        return companies.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchLocations(GlobalSearchRequest request) {
        List<LocationDocument> locations;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            locations = locationSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            locations = (List<LocationDocument>) locationSearchRepository.findAll();
        }

        return locations.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchZones(GlobalSearchRequest request) {
        List<ZoneDocument> zones;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            zones = zoneSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            zones = (List<ZoneDocument>) zoneSearchRepository.findAll();
        }

        return zones.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchSensors(GlobalSearchRequest request) {
        List<SensorDocument> sensors;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            sensors = sensorSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            sensors = (List<SensorDocument>) sensorSearchRepository.findAll();
        }

        return sensors.stream()
                .map(this::toSearchResultItem)
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private boolean shouldSearchEntity(GlobalSearchRequest request, String entityType) {
        return request.getEntityTypes() == null
                || request.getEntityTypes().isEmpty()
                || request.getEntityTypes().contains(entityType);
    }

    private Pageable createPageable(GlobalSearchRequest request) {
        Sort sort = Sort.unsorted();
        if (request.getSortBy() != null) {
            Sort.Direction direction = "DESC".equalsIgnoreCase(request.getSortDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, request.getSortBy());
        }
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(CompanyDocument doc) {
        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("COMPANY")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(1.0)
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "industry", doc.getIndustry() != null ? doc.getIndustry() : "",
                        "city", doc.getCity() != null ? doc.getCity() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(LocationDocument doc) {
        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("LOCATION")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.9)
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "companyId", doc.getCompanyId(),
                        "city", doc.getCity() != null ? doc.getCity() : "",
                        "country", doc.getCountry() != null ? doc.getCountry() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(ZoneDocument doc) {
        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("ZONE")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.8)
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "locationId", doc.getLocationId(),
                        "type", doc.getType() != null ? doc.getType() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(SensorDocument doc) {
        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("SENSOR")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.7)
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "serialNumber", doc.getSerialNumber(),
                        "sensorType", doc.getSensorType(),
                        "zoneId", doc.getZoneId() != null ? doc.getZoneId() : 0L
                ))
                .build();
    }
}
