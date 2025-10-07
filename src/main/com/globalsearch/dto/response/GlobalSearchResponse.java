package com.globalsearch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {

    private List<SearchResultItem> results;

    private Long totalResults;

    private Integer currentPage;

    private Integer totalPages;

    private Integer pageSize;

    private Long searchDurationMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {
        private String entityType; // COMPANY, LOCATION, ZONE, SENSOR
        private Long id;
        private String name;
        private String description;
        private String status;
        private Object metadata; // Additional entity-specific data
        private Double relevanceScore;
    }
}
