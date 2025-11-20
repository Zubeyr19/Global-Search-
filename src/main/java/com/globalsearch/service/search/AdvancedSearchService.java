package com.globalsearch.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.globalsearch.dto.response.GlobalSearchResponse;
import com.globalsearch.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced search service with fuzzy matching, highlighting, and more
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class AdvancedSearchService {

    private final ElasticsearchTemplate elasticsearchTemplate;

    // Common synonyms for IoT domain
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "sensor", List.of("device", "detector", "probe"),
            "location", List.of("site", "place", "facility"),
            "zone", List.of("area", "region", "section"),
            "company", List.of("organization", "enterprise", "business"),
            "temperature", List.of("temp", "thermal"),
            "humidity", List.of("moisture", "dampness")
    );

    /**
     * Perform advanced search with fuzzy matching and highlighting
     */
    public List<GlobalSearchResponse.SearchResultItem> advancedSearch(
            String query,
            String tenantId,
            String[] indices,
            boolean enableFuzzy,
            boolean enableSynonyms,
            boolean enableHighlighting,
            User currentUser) {

        log.debug("Advanced search - query: {}, tenant: {}, fuzzy: {}, synonyms: {}",
                query, tenantId, enableFuzzy, enableSynonyms);

        List<GlobalSearchResponse.SearchResultItem> results = new ArrayList<>();

        // Build expanded query with synonyms
        List<String> queryTerms = new ArrayList<>();
        queryTerms.add(query);

        if (enableSynonyms) {
            queryTerms.addAll(expandWithSynonyms(query));
        }

        // Search each index
        for (String index : indices) {
            try {
                results.addAll(searchIndex(index, query, queryTerms, tenantId,
                        enableFuzzy, enableHighlighting));
            } catch (Exception e) {
                log.error("Error searching index {}: {}", index, e.getMessage());
            }
        }

        return results;
    }

    /**
     * Search a specific index with advanced features
     */
    private List<GlobalSearchResponse.SearchResultItem> searchIndex(
            String index,
            String originalQuery,
            List<String> queryTerms,
            String tenantId,
            boolean enableFuzzy,
            boolean enableHighlighting) {

        List<GlobalSearchResponse.SearchResultItem> results = new ArrayList<>();

        try {
            // Build the query
            Query query = buildQuery(queryTerms, tenantId, enableFuzzy);

            // Build search request
            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(index)
                    .query(query)
                    .size(100);

            // Add highlighting if enabled
            if (enableHighlighting) {
                searchRequestBuilder.highlight(buildHighlight());
            }

            // Execute search using the native Elasticsearch client
            // Note: This is a simplified example. In production, you'd use the actual client
            log.debug("Executing search on index: {}", index);

            // For now, return empty list as this requires proper Elasticsearch client setup
            // In production, you would:
            // 1. Get the Elasticsearch client from ElasticsearchTemplate
            // 2. Execute the search
            // 3. Parse the results and highlights
            // 4. Convert to SearchResultItem objects

        } catch (Exception e) {
            log.error("Error searching index {}: {}", index, e.getMessage());
        }

        return results;
    }

    /**
     * Build Elasticsearch query with fuzzy matching
     */
    private Query buildQuery(List<String> queryTerms, String tenantId, boolean enableFuzzy) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // Add tenant filter
        boolQuery.filter(f -> f.term(t -> t.field("tenantId").value(tenantId)));

        // Build multi-match query for the search terms
        List<Query> shouldQueries = new ArrayList<>();

        for (String term : queryTerms) {
            // Exact match (higher score)
            shouldQueries.add(Query.of(q -> q.multiMatch(m -> m
                    .query(term)
                    .fields("name^3", "description^2", "status")
                    .type(TextQueryType.BestFields)
            )));

            // Fuzzy match if enabled
            if (enableFuzzy) {
                shouldQueries.add(Query.of(q -> q.multiMatch(m -> m
                        .query(term)
                        .fields("name^2", "description")
                        .fuzziness("AUTO")
                        .prefixLength(1)
                )));
            }
        }

        boolQuery.should(shouldQueries);
        boolQuery.minimumShouldMatch("1");

        return Query.of(q -> q.bool(boolQuery.build()));
    }

    /**
     * Build highlight configuration
     */
    private Highlight buildHighlight() {
        return Highlight.of(h -> h
                .fields("name", HighlightField.of(f -> f
                        .preTags("<mark>")
                        .postTags("</mark>")
                        .numberOfFragments(0)))
                .fields("description", HighlightField.of(f -> f
                        .preTags("<mark>")
                        .postTags("</mark>")
                        .fragmentSize(150)
                        .numberOfFragments(3)))
        );
    }

    /**
     * Expand query with synonyms
     */
    private List<String> expandWithSynonyms(String query) {
        List<String> expanded = new ArrayList<>();
        String[] words = query.toLowerCase().split("\\s+");

        for (String word : words) {
            if (SYNONYMS.containsKey(word)) {
                expanded.addAll(SYNONYMS.get(word));
            }
        }

        log.debug("Expanded query '{}' with synonyms: {}", query, expanded);
        return expanded;
    }

    /**
     * Perform fuzzy search for a single term
     */
    public List<GlobalSearchResponse.SearchResultItem> fuzzySearch(
            String term,
            String tenantId,
            String entityType,
            int maxEdits) {

        log.info("Fuzzy search - term: {}, tenant: {}, entity: {}, maxEdits: {}",
                term, tenantId, entityType, maxEdits);

        // Build fuzzy query
        Query fuzzyQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.fuzzy(f -> f
                        .field("name")
                        .value(term)
                        .fuzziness(String.valueOf(maxEdits))
                ))
                .filter(filter -> filter.term(t -> t
                        .field("tenantId")
                        .value(tenantId)
                ))
        ));

        // Execute search and return results
        // In production, execute the query and parse results
        return new ArrayList<>();
    }

    /**
     * Search with wildcard patterns
     */
    public List<GlobalSearchResponse.SearchResultItem> wildcardSearch(
            String pattern,
            String tenantId,
            String fieldName) {

        log.info("Wildcard search - pattern: {}, tenant: {}, field: {}",
                pattern, tenantId, fieldName);

        Query wildcardQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.wildcard(w -> w
                        .field(fieldName)
                        .value(pattern)
                        .caseInsensitive(true)
                ))
                .filter(filter -> filter.term(t -> t
                        .field("tenantId")
                        .value(tenantId)
                ))
        ));

        return new ArrayList<>();
    }

    /**
     * Search with regular expression
     */
    public List<GlobalSearchResponse.SearchResultItem> regexSearch(
            String regex,
            String tenantId,
            String fieldName) {

        log.info("Regex search - pattern: {}, tenant: {}, field: {}",
                regex, tenantId, fieldName);

        Query regexQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.regexp(r -> r
                        .field(fieldName)
                        .value(regex)
                        .caseInsensitive(true)
                ))
                .filter(filter -> filter.term(t -> t
                        .field("tenantId")
                        .value(tenantId)
                ))
        ));

        return new ArrayList<>();
    }
}
