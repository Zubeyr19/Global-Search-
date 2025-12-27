package com.globalsearch.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for search operations
 * Provides fuzzy matching, highlighting, and synonym expansion
 */
@Slf4j
public class SearchUtils {

    // Common synonyms for IoT domain
    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "sensor", List.of("device", "detector", "probe", "monitor"),
            "location", List.of("site", "place", "facility", "building"),
            "zone", List.of("area", "region", "section", "space"),
            "company", List.of("organization", "enterprise", "business", "firm"),
            "temperature", List.of("temp", "thermal", "heat"),
            "humidity", List.of("moisture", "dampness", "wetness"),
            "pressure", List.of("force", "compression"),
            "active", List.of("enabled", "online", "operational", "running"),
            "inactive", List.of("disabled", "offline", "stopped"),
            "data", List.of("information", "readings", "measurements")
    );

    /**
     * Apply HTML highlighting to matched terms in text
     */
    public static String highlight(String text, String searchTerm) {
        if (text == null || searchTerm == null || searchTerm.isEmpty()) {
            return text;
        }

        try {
            // Escape special regex characters in search term
            String escapedTerm = Pattern.quote(searchTerm);

            // Create pattern for case-insensitive matching
            Pattern pattern = Pattern.compile(escapedTerm, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            // Replace matches with highlighted version
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(result, "<mark>" + matcher.group() + "</mark>");
            }
            matcher.appendTail(result);

            return result.toString();
        } catch (Exception e) {
            log.warn("Error highlighting text: {}", e.getMessage());
            return text;
        }
    }

    /**
     * Apply highlighting for multiple search terms
     */
    public static String highlightMultiple(String text, List<String> searchTerms) {
        if (text == null || searchTerms == null || searchTerms.isEmpty()) {
            return text;
        }

        String result = text;
        for (String term : searchTerms) {
            result = highlight(result, term);
        }
        return result;
    }

    /**
     * Calculate Levenshtein distance between two strings
     * Used for fuzzy matching
     */
    public static int levenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }

        return costs[s2.length()];
    }

    /**
     * Check if two strings are within fuzzy distance
     */
    public static boolean isFuzzyMatch(String text, String searchTerm, int maxDistance) {
        if (text == null || searchTerm == null) {
            return false;
        }

        // Direct match
        if (text.toLowerCase().contains(searchTerm.toLowerCase())) {
            return true;
        }

        // Check Levenshtein distance for whole string
        int distance = levenshteinDistance(text, searchTerm);
        if (distance <= maxDistance) {
            return true;
        }

        // Check each word in text
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            if (levenshteinDistance(word, searchTerm.toLowerCase()) <= maxDistance) {
                return true;
            }
        }

        return false;
    }

    /**
     * Expand search query with synonyms
     */
    public static List<String> expandWithSynonyms(String query) {
        List<String> expanded = new ArrayList<>();
        expanded.add(query);

        String[] words = query.toLowerCase().split("\\s+");

        for (String word : words) {
            if (SYNONYMS.containsKey(word)) {
                List<String> synonyms = SYNONYMS.get(word);
                // Add single-word synonyms
                for (String synonym : synonyms) {
                    if (!synonym.contains(" ")) {
                        expanded.add(synonym);
                    }
                }
            }
        }

        log.debug("Expanded query '{}' with synonyms: {}", query, expanded);
        return expanded;
    }

    /**
     * Get all matched terms from a list of possible search terms
     */
    public static List<String> getMatchedTerms(String text, List<String> searchTerms) {
        List<String> matched = new ArrayList<>();

        if (text == null || searchTerms == null) {
            return matched;
        }

        String lowerText = text.toLowerCase();
        for (String term : searchTerms) {
            if (lowerText.contains(term.toLowerCase())) {
                matched.add(term);
            }
        }

        return matched;
    }

    /**
     * Calculate relevance score based on match quality
     */
    public static double calculateRelevanceScore(
            String text,
            String searchTerm,
            boolean isExactMatch,
            boolean isFuzzyMatch,
            int fieldImportance) {

        double score = 1.0;

        // Boost for exact matches
        if (isExactMatch) {
            score *= 2.0;
        }

        // Penalty for fuzzy matches
        if (isFuzzyMatch && !isExactMatch) {
            score *= 0.7;
        }

        // Boost based on field importance (1-5)
        score *= (fieldImportance / 3.0);

        // Boost if match is at the beginning of text
        if (text != null && searchTerm != null &&
                text.toLowerCase().startsWith(searchTerm.toLowerCase())) {
            score *= 1.5;
        }

        return Math.min(score, 10.0); // Cap at 10.0
    }

    /**
     * Check if text contains any of the search terms
     */
    public static boolean containsAny(String text, List<String> searchTerms) {
        if (text == null || searchTerms == null || searchTerms.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return searchTerms.stream()
                .anyMatch(term -> lowerText.contains(term.toLowerCase()));
    }

    /**
     * Get snippet of text around matched term
     */
    public static String getSnippet(String text, String searchTerm, int contextLength) {
        if (text == null || searchTerm == null || text.length() <= contextLength) {
            return text;
        }

        int index = text.toLowerCase().indexOf(searchTerm.toLowerCase());
        if (index == -1) {
            return text.substring(0, Math.min(contextLength, text.length())) + "...";
        }

        int start = Math.max(0, index - contextLength / 2);
        int end = Math.min(text.length(), index + searchTerm.length() + contextLength / 2);

        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";

        return snippet;
    }
}
