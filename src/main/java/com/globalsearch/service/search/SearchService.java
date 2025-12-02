package com.globalsearch.service.search;

import com.globalsearch.document.*;
import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.response.GlobalSearchResponse;
import com.globalsearch.entity.User;
import com.globalsearch.repository.search.*;
import com.globalsearch.dto.WebSocketMessage;
import com.globalsearch.service.AuditLogService;
import com.globalsearch.service.NotificationService;
import com.globalsearch.service.PerformanceMetricsService;
import com.globalsearch.util.SearchUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class SearchService {

    private final CompanySearchRepository companySearchRepository;
    private final LocationSearchRepository locationSearchRepository;
    private final ZoneSearchRepository zoneSearchRepository;
    private final SensorSearchRepository sensorSearchRepository;
    private final ReportSearchRepository reportSearchRepository;
    private final DashboardSearchRepository dashboardSearchRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PerformanceMetricsService performanceMetricsService;

    /**
     * Global search with document-level security enforcement
     * Users only see data they have access to based on tenantId and roles
     */

    @Cacheable(
            value = "searchResults",
            key = "#request.query + '_' + #currentUser.tenantId + '_' + #request.page + '_' + #request.size",
            unless = "#result.totalResults == 0"
    )


    public GlobalSearchResponse globalSearch(
            GlobalSearchRequest request,
            User currentUser,
            HttpServletRequest httpRequest) {

        long startTime = System.currentTimeMillis();
        log.debug("Cache MISS - Executing search for: {}", request.getQuery());

        log.debug("Global search for user: {}, tenant: {}, query: {}",
                currentUser.getUsername(), currentUser.getTenantId(), request.getQuery());

        // Check if user is SUPER_ADMIN or tenant is SYSTEM - they should search across all tenants
        boolean isSuperAdmin = currentUser.hasRole(User.Role.SUPER_ADMIN) || "SYSTEM".equals(currentUser.getTenantId());

        log.info("User: {}, Tenant: {}, Has SUPER_ADMIN role: {}, Tenant is SYSTEM: {}, isSuperAdmin: {}",
                 currentUser.getUsername(), currentUser.getTenantId(),
                 currentUser.hasRole(User.Role.SUPER_ADMIN), "SYSTEM".equals(currentUser.getTenantId()), isSuperAdmin);

        if (isSuperAdmin) {
            log.info("SUPER_ADMIN detected - routing to cross-tenant search for user: {}", currentUser.getUsername());
            return adminGlobalSearch(request, currentUser, httpRequest);
        }

        // Create pageable
        Pageable pageable = createPageable(request);

        // Expand query with synonyms if enabled
        List<String> searchTerms = new ArrayList<>();
        searchTerms.add(request.getQuery());
        if (Boolean.TRUE.equals(request.getEnableSynonyms())) {
            searchTerms.addAll(SearchUtils.expandWithSynonyms(request.getQuery()));
            log.debug("Expanded search with synonyms: {}", searchTerms);
        }

        // Collect all results
        List<GlobalSearchResponse.SearchResultItem> allResults = new ArrayList<>();

        // Search entities based on user permissions
        if (shouldSearchEntity(request, "companies")) {
            allResults.addAll(searchCompanies(request, currentUser, searchTerms));
        }

        if (shouldSearchEntity(request, "locations")) {
            allResults.addAll(searchLocations(request, currentUser, searchTerms));
        }

        if (shouldSearchEntity(request, "zones")) {
            allResults.addAll(searchZones(request, currentUser, searchTerms));
        }

        if (shouldSearchEntity(request, "sensors")) {
            allResults.addAll(searchSensors(request, currentUser, searchTerms));
        }

        if (shouldSearchEntity(request, "reports")) {
            allResults.addAll(searchReports(request, currentUser, searchTerms));
        }

        if (shouldSearchEntity(request, "dashboards")) {
            allResults.addAll(searchDashboards(request, currentUser, searchTerms));
        }

        // Sort by relevance or specified field
        allResults.sort(Comparator.comparing(GlobalSearchResponse.SearchResultItem::getRelevanceScore).reversed());

        // Apply pagination
        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), allResults.size());
        List<GlobalSearchResponse.SearchResultItem> paginatedResults =
                allResults.subList(Math.min(start, allResults.size()), end);

        long duration = System.currentTimeMillis() - startTime;

        // Record performance metrics
        performanceMetricsService.recordQueryExecution(
                currentUser.getTenantId(),
                "global_search",
                duration
        );

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

    @Cacheable(
            value = "searchResults",
            key = "'admin_' + #request.query + '_' + #request.page + '_' + #request.size"
    )
    public GlobalSearchResponse adminGlobalSearch(GlobalSearchRequest request, User admin, HttpServletRequest httpRequest) {
        long startTime = System.currentTimeMillis();
        log.debug("Cache MISS - Executing admin search for: {}", request.getQuery());

        log.debug("Admin global search by: {}, query: {}", admin.getUsername(), request.getQuery());

        // Verify admin role
        if (!admin.isAdmin()) {
            throw new SecurityException("User does not have admin privileges");
        }

        // Expand query with synonyms if enabled
        List<String> searchTerms = new ArrayList<>();
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            searchTerms.add(request.getQuery());
            if (Boolean.TRUE.equals(request.getEnableSynonyms())) {
                searchTerms.addAll(SearchUtils.expandWithSynonyms(request.getQuery()));
                log.debug("Admin search - Expanded with synonyms: {}", searchTerms);
            }
        }

        // Admin can search without tenant restriction
        List<GlobalSearchResponse.SearchResultItem> allResults = new ArrayList<>();

        if (shouldSearchEntity(request, "companies")) {
            allResults.addAll(adminSearchCompanies(request, searchTerms));
        }

        if (shouldSearchEntity(request, "locations")) {
            allResults.addAll(adminSearchLocations(request, searchTerms));
        }

        if (shouldSearchEntity(request, "zones")) {
            allResults.addAll(adminSearchZones(request, searchTerms));
        }

        if (shouldSearchEntity(request, "sensors")) {
            allResults.addAll(adminSearchSensors(request, searchTerms));
        }

        if (shouldSearchEntity(request, "reports")) {
            allResults.addAll(adminSearchReports(request, searchTerms));
        }

        if (shouldSearchEntity(request, "dashboards")) {
            allResults.addAll(adminSearchDashboards(request, searchTerms));
        }

        // Sort and paginate
        allResults.sort(Comparator.comparing(GlobalSearchResponse.SearchResultItem::getRelevanceScore).reversed());

        int start = request.getPage() * request.getSize();
        int end = Math.min(start + request.getSize(), allResults.size());
        List<GlobalSearchResponse.SearchResultItem> paginatedResults =
                allResults.subList(Math.min(start, allResults.size()), end);

        long duration = System.currentTimeMillis() - startTime;

        // Record performance metrics
        performanceMetricsService.recordQueryExecution(
                admin.getTenantId(),
                "admin_cross_tenant_search",
                duration
        );

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

    private List<GlobalSearchResponse.SearchResultItem> searchCompanies(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<CompanyDocument> companies;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            companies = companySearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<CompanyDocument> fuzzyResults = companySearchRepository
                        .findByNameFuzzyAndTenantId(request.getQuery(), user.getTenantId());

                for (CompanyDocument company : fuzzyResults) {
                    if (!companies.contains(company)) {
                        companies.add(company);
                    }
                }
            }
        } else {
            companies = companySearchRepository.findByTenantId(user.getTenantId());
        }

        return companies.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchLocations(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<LocationDocument> locations;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            locations = locationSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<LocationDocument> allLocations = locationSearchRepository.findByTenantId(user.getTenantId());
                for (LocationDocument location : allLocations) {
                    boolean fuzzyMatch = SearchUtils.isFuzzyMatch(
                            location.getName(), request.getQuery(), request.getFuzzyMaxEdits());
                    if (fuzzyMatch && !locations.contains(location)) {
                        locations.add(location);
                    }
                }
            }
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
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchZones(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<ZoneDocument> zones;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            zones = zoneSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<ZoneDocument> allZones = zoneSearchRepository.findByTenantId(user.getTenantId());
                for (ZoneDocument zone : allZones) {
                    boolean fuzzyMatch = SearchUtils.isFuzzyMatch(
                            zone.getName(), request.getQuery(), request.getFuzzyMaxEdits());
                    if (fuzzyMatch && !zones.contains(zone)) {
                        zones.add(zone);
                    }
                }
            }
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
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchSensors(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<SensorDocument> sensors;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            sensors = sensorSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<SensorDocument> allSensors = sensorSearchRepository.findByTenantId(user.getTenantId());
                for (SensorDocument sensor : allSensors) {
                    boolean fuzzyMatch = SearchUtils.isFuzzyMatch(
                            sensor.getName(), request.getQuery(), request.getFuzzyMaxEdits());
                    if (fuzzyMatch && !sensors.contains(sensor)) {
                        sensors.add(sensor);
                    }
                }
            }
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
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchReports(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<ReportDocument> reports;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            reports = reportSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<ReportDocument> allReports = reportSearchRepository.findByTenantId(user.getTenantId());
                for (ReportDocument report : allReports) {
                    boolean fuzzyMatch = SearchUtils.isFuzzyMatch(
                            report.getName(), request.getQuery(), request.getFuzzyMaxEdits());
                    if (fuzzyMatch && !reports.contains(report)) {
                        reports.add(report);
                    }
                }
            }
        } else {
            reports = reportSearchRepository.findByTenantId(user.getTenantId());
        }

        return reports.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> searchDashboards(
            GlobalSearchRequest request, User user, List<String> searchTerms) {
        List<DashboardDocument> dashboards;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            dashboards = dashboardSearchRepository.findByTenantIdAndNameContainingIgnoreCase(
                    user.getTenantId(), request.getQuery());

            // Apply fuzzy search if enabled
            if (Boolean.TRUE.equals(request.getEnableFuzzySearch())) {
                List<DashboardDocument> allDashboards = dashboardSearchRepository.findByTenantId(user.getTenantId());
                for (DashboardDocument dashboard : allDashboards) {
                    boolean fuzzyMatch = SearchUtils.isFuzzyMatch(
                            dashboard.getName(), request.getQuery(), request.getFuzzyMaxEdits());
                    if (fuzzyMatch && !dashboards.contains(dashboard)) {
                        dashboards.add(dashboard);
                    }
                }
            }
        } else {
            dashboards = dashboardSearchRepository.findByTenantId(user.getTenantId());
        }

        return dashboards.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    // ==================== ADMIN SEARCH METHODS (NO TENANT RESTRICTION) ====================

    private List<GlobalSearchResponse.SearchResultItem> adminSearchCompanies(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<CompanyDocument> companies;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            companies = companySearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<CompanyDocument> page = companySearchRepository.findAll(pageRequest);
            companies = page.getContent();
        }

        return companies.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchLocations(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<LocationDocument> locations;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            locations = locationSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<LocationDocument> page = locationSearchRepository.findAll(pageRequest);
            locations = page.getContent();
        }

        return locations.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchZones(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<ZoneDocument> zones;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            zones = zoneSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<ZoneDocument> page = zoneSearchRepository.findAll(pageRequest);
            zones = page.getContent();
        }

        return zones.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchSensors(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<SensorDocument> sensors;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            sensors = sensorSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<SensorDocument> page = sensorSearchRepository.findAll(pageRequest);
            sensors = page.getContent();
        }

        return sensors.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchReports(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<ReportDocument> reports;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            reports = reportSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<ReportDocument> page = reportSearchRepository.findAll(pageRequest);
            reports = page.getContent();
        }

        return reports.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
                .collect(Collectors.toList());
    }

    private List<GlobalSearchResponse.SearchResultItem> adminSearchDashboards(
            GlobalSearchRequest request, List<String> searchTerms) {
        List<DashboardDocument> dashboards;

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            dashboards = dashboardSearchRepository.findByNameContainingIgnoreCase(request.getQuery());
        } else {
            // Instead of findAll(), use pagination with reasonable limit
            PageRequest pageRequest = PageRequest.of(
                request.getPage(),
                Math.min(request.getSize(), 1000)  // Max 1000 results
            );
            Page<DashboardDocument> page = dashboardSearchRepository.findAll(pageRequest);
            dashboards = page.getContent();
        }

        return dashboards.stream()
                .map(doc -> toSearchResultItem(doc, request, searchTerms))
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

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            CompanyDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("COMPANY")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(1.0)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "industry", doc.getIndustry() != null ? doc.getIndustry() : "",
                        "city", doc.getCity() != null ? doc.getCity() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            LocationDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("LOCATION")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.9)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "companyId", doc.getCompanyId(),
                        "city", doc.getCity() != null ? doc.getCity() : "",
                        "country", doc.getCountry() != null ? doc.getCountry() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            ZoneDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("ZONE")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.8)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "locationId", doc.getLocationId(),
                        "type", doc.getType() != null ? doc.getType() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            SensorDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("SENSOR")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status(doc.getStatus())
                .relevanceScore(0.7)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "serialNumber", doc.getSerialNumber(),
                        "sensorType", doc.getSensorType(),
                        "zoneId", doc.getZoneId() != null ? doc.getZoneId() : 0L
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            ReportDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("REPORT")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status("ACTIVE")
                .relevanceScore(0.6)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "reportType", doc.getReportType() != null ? doc.getReportType() : "",
                        "createdBy", doc.getCreatedBy() != null ? doc.getCreatedBy() : ""
                ))
                .build();
    }

    private GlobalSearchResponse.SearchResultItem toSearchResultItem(
            DashboardDocument doc, GlobalSearchRequest request, List<String> searchTerms) {
        boolean enableHighlighting = Boolean.TRUE.equals(request.getEnableHighlighting());

        return GlobalSearchResponse.SearchResultItem.builder()
                .entityType("DASHBOARD")
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .status("ACTIVE")
                .relevanceScore(0.6)
                .highlightedName(enableHighlighting ? SearchUtils.highlightMultiple(doc.getName(), searchTerms) : null)
                .highlightedDescription(enableHighlighting ? SearchUtils.highlightMultiple(doc.getDescription(), searchTerms) : null)
                .matchedTerms(SearchUtils.getMatchedTerms(doc.getName() + " " + doc.getDescription(), searchTerms))
                .isFuzzyMatch(Boolean.TRUE.equals(request.getEnableFuzzySearch()) &&
                        SearchUtils.isFuzzyMatch(doc.getName(), request.getQuery(), request.getFuzzyMaxEdits()))
                .metadata(Map.of(
                        "tenantId", doc.getTenantId(),
                        "dashboardType", doc.getDashboardType() != null ? doc.getDashboardType() : "",
                        "isShared", doc.getIsShared() != null ? doc.getIsShared().toString() : "false"
                ))
                .build();
    }

}
