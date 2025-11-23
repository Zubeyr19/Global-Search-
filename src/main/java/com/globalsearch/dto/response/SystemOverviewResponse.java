package com.globalsearch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewResponse {
    private Long totalUsers;
    private Long totalCompanies;
    private Long totalLocations;
    private Long totalZones;
    private Long totalSensors;
    private Long totalSearches;
    private Long totalTenants;
    private Long activeUsers; // Active in last 24 hours
    private Long totalReports;
    private Long totalDashboards;

    private Map<String, Long> usersByRole;
    private Map<String, Long> companiesByStatus;
    private List<RecentActivityDTO> recentActivity;

    // Performance metrics
    private Double averageSearchTime;
    private Long totalAuditLogs;
    private Map<String, Long> searchesByDay;
}
