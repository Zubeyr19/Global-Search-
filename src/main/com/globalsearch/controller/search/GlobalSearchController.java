package com.globalsearch.controller.search;

import com.globalsearch.dto.request.GlobalSearchRequest;
import com.globalsearch.dto.response.GlobalSearchResponse;
import com.globalsearch.entity.User;
import com.globalsearch.service.auth.CustomUserDetailsService;
import com.globalsearch.service.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class GlobalSearchController {

    private final SearchService searchService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Global search endpoint for regular users
     * Users can only search within their tenant
     * Supports filtering, pagination, and sorting
     */
    @PostMapping
    public ResponseEntity<?> search(@RequestBody GlobalSearchRequest request) {
        try {
            User currentUser = getCurrentUser();

            log.info("Search request from user: {}, tenant: {}, query: {}",
                    currentUser.getUsername(), currentUser.getTenantId(), request.getQuery());

            GlobalSearchResponse response = searchService.globalSearch(request, currentUser);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("Security error during search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        } catch (Exception e) {
            log.error("Error during search: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed", "message", e.getMessage()));
        }
    }

    /**
     * Quick search endpoint - simplified version for autocomplete
     */
    @GetMapping("/quick")
    public ResponseEntity<?> quickSearch(@RequestParam String query) {
        try {
            User currentUser = getCurrentUser();

            GlobalSearchRequest request = GlobalSearchRequest.builder()
                    .query(query)
                    .page(0)
                    .size(10)
                    .build();

            GlobalSearchResponse response = searchService.globalSearch(request, currentUser);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during quick search: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed"));
        }
    }

    /**
     * Search within specific entity type
     */
    @GetMapping("/{entityType}")
    public ResponseEntity<?> searchByEntityType(
            @PathVariable String entityType,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        try {
            User currentUser = getCurrentUser();

            GlobalSearchRequest request = GlobalSearchRequest.builder()
                    .query(query)
                    .entityTypes(java.util.List.of(entityType))
                    .page(page)
                    .size(size)
                    .build();

            GlobalSearchResponse response = searchService.globalSearch(request, currentUser);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during entity type search: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed"));
        }
    }

    /**
     * Admin-only endpoint - search across all tenants
     */
    @PostMapping("/admin")
    public ResponseEntity<?> adminSearch(@RequestBody GlobalSearchRequest request) {
        try {
            User currentUser = getCurrentUser();

            // Verify admin role
            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Admin privileges required"));
            }

            log.info("Admin search by: {}, query: {}", currentUser.getUsername(), request.getQuery());

            GlobalSearchResponse response = searchService.adminGlobalSearch(request, currentUser);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.error("Security error during admin search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during admin search: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Search failed", "message", e.getMessage()));
        }
    }

    /**
     * Get search statistics for current user
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSearchStats() {
        try {
            User currentUser = getCurrentUser();

            // TODO: Implement search statistics tracking
            Map<String, Object> stats = Map.of(
                    "userId", currentUser.getId(),
                    "tenantId", currentUser.getTenantId(),
                    "message", "Stats tracking coming soon"
            );

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching search stats: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stats"));
        }
    }

    // Helper method to get current authenticated user
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String username = authentication.getName();
        return userDetailsService.loadUserEntityByUsername(username);
    }
}
