package com.globalsearch.controller;

import com.globalsearch.service.ElasticsearchSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for manually triggering Elasticsearch synchronization.
 * Only accessible by SUPER_ADMIN users.
 */
@RestController
@RequestMapping("/api/admin/elasticsearch")
@RequiredArgsConstructor
@Tag(name = "Elasticsearch Sync", description = "Elasticsearch synchronization endpoints")
public class ElasticsearchSyncController {

    private final ElasticsearchSyncService syncService;

    /**
     * Manually trigger a full synchronization of all data from MySQL to Elasticsearch.
     *
     * @return Success message
     */
    @PostMapping("/sync/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync all data to Elasticsearch", description = "Manually trigger synchronization of all entities from MySQL to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncAll() {
        syncService.manualFullSync();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Full Elasticsearch synchronization started");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only companies to Elasticsearch.
     */
    @PostMapping("/sync/companies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync companies only", description = "Synchronize all companies to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncCompanies() {
        syncService.syncAllCompanies();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Companies synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only locations to Elasticsearch.
     */
    @PostMapping("/sync/locations")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync locations only", description = "Synchronize all locations to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncLocations() {
        syncService.syncAllLocations();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Locations synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only zones to Elasticsearch.
     */
    @PostMapping("/sync/zones")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync zones only", description = "Synchronize all zones to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncZones() {
        syncService.syncAllZones();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Zones synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only sensors to Elasticsearch.
     */
    @PostMapping("/sync/sensors")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync sensors only", description = "Synchronize all sensors to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncSensors() {
        syncService.syncAllSensors();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Sensors synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only reports to Elasticsearch.
     */
    @PostMapping("/sync/reports")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync reports only", description = "Synchronize all reports to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncReports() {
        syncService.syncAllReports();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Reports synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }

    /**
     * Sync only dashboards to Elasticsearch.
     */
    @PostMapping("/sync/dashboards")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Sync dashboards only", description = "Synchronize all dashboards to Elasticsearch")
    public ResponseEntity<Map<String, String>> syncDashboards() {
        syncService.syncAllDashboards();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Dashboards synchronized to Elasticsearch");
        response.put("status", "SUCCESS");

        return ResponseEntity.ok(response);
    }
}
