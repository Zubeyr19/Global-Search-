package com.globalsearch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchRequest {

    private String query;

    private List<String> entityTypes; // companies, locations, zones, sensors

    private String city;

    private String country;

    private String status;

    private String sensorType;

    private Long companyId;

    private Long locationId;

    private Long zoneId;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    private String sortBy;

    @Builder.Default
    private String sortDirection = "ASC";

    // Advanced search options
    @Builder.Default
    private Boolean enableFuzzySearch = false;

    @Builder.Default
    private Boolean enableSynonyms = false;

    @Builder.Default
    private Boolean enableHighlighting = false;

    @Builder.Default
    private Integer fuzzyMaxEdits = 1; // 0, 1, or 2

    @Builder.Default
    private Integer fuzzyPrefixLength = 1;
}
