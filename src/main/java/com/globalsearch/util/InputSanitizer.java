package com.globalsearch.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for input sanitization and validation
 * Prevents XSS, SQL injection, path traversal, and other injection attacks
 */
@Component
@Slf4j
public class InputSanitizer {

    // XSS patterns to detect
    private static final Pattern XSS_SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern XSS_EVENT_PATTERN = Pattern.compile(
        "on\\w+\\s*=", Pattern.CASE_INSENSITIVE
    );
    private static final Pattern XSS_IFRAME_PATTERN = Pattern.compile(
        "<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // SQL injection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE|FROM|WHERE|OR|AND|TABLE)\\b)" +
        "|(-{2})|(/\\*.*?\\*/)" +
        "|(;\\s*(DROP|DELETE|UPDATE|INSERT))" +
        "|(\\bOR\\s+\\w+\\s*=\\s*\\w+)|('.*--)",
        Pattern.CASE_INSENSITIVE
    );

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./)|(\\.\\\\)|(%2e%2e%2f)|(%2e%2e/)|(%2e%2e\\\\)",
        Pattern.CASE_INSENSITIVE
    );

    // Command injection patterns
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "[;&|`$(){}\\[\\]<>]"
    );

    // LDAP injection patterns
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
        "[*()|&]"
    );

    /**
     * Sanitize input for general text fields
     * Removes HTML tags and dangerous characters
     */
    public String sanitizeText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove HTML tags
        String sanitized = input.replaceAll("<[^>]*>", "");

        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[<>\"'`]", "");

        // Trim whitespace
        sanitized = sanitized.trim();

        return sanitized;
    }

    /**
     * Sanitize input for search queries
     * Allows more characters but removes dangerous patterns
     */
    public String sanitizeSearchQuery(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("Potential SQL injection attempt detected in search query: {}", input);
            // Remove dangerous SQL keywords
            input = SQL_INJECTION_PATTERN.matcher(input).replaceAll("");
        }

        // Remove script tags
        input = XSS_SCRIPT_PATTERN.matcher(input).replaceAll("");

        // Remove event handlers
        input = XSS_EVENT_PATTERN.matcher(input).replaceAll("");

        return input.trim();
    }

    /**
     * Validate filename for uploads
     * Prevents path traversal attacks
     */
    public boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        // Check for path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(filename).find()) {
            log.warn("Path traversal attempt detected in filename: {}", filename);
            return false;
        }

        // Check for null bytes
        if (filename.contains("\0")) {
            log.warn("Null byte detected in filename: {}", filename);
            return false;
        }

        // Only allow alphanumeric, dash, underscore, and dot
        return filename.matches("^[a-zA-Z0-9._-]+$");
    }

    /**
     * Sanitize filename by removing dangerous characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }

        // Remove null bytes
        filename = filename.replace("\0", "");

        // Remove consecutive dots (path traversal attempt) - do this BEFORE removing separators
        filename = filename.replaceAll("\\.{2,}", "");

        // Replace path separators with underscore
        filename = filename.replaceAll("[/\\\\]", "_");

        // Keep only safe characters
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Remove leading/trailing underscores
        filename = filename.replaceAll("^_+|_+$", "");

        return filename;
    }

    /**
     * Validate email address format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex) && email.length() <= 100;
    }

    /**
     * Validate username format
     * Only alphanumeric, dots, underscores, and hyphens allowed
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        return username.matches("^[a-zA-Z0-9._-]{3,50}$");
    }

    /**
     * Check for XSS attempts in input
     */
    public boolean containsXSS(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return XSS_SCRIPT_PATTERN.matcher(input).find() ||
               XSS_EVENT_PATTERN.matcher(input).find() ||
               XSS_IFRAME_PATTERN.matcher(input).find();
    }

    /**
     * Check for SQL injection attempts
     */
    public boolean containsSQLInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check for path traversal attempts
     */
    public boolean containsPathTraversal(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Check for command injection attempts
     */
    public boolean containsCommandInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitize HTML content
     * Escapes HTML special characters
     */
    public String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;")
                    .replace("/", "&#x2F;");
    }

    /**
     * Validate tenant ID format
     */
    public boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }

        // Tenant ID should be alphanumeric and underscores only
        return tenantId.matches("^[A-Z0-9_]{2,50}$");
    }

    /**
     * Validate numeric ID
     */
    public boolean isValidNumericId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        try {
            Long value = Long.parseLong(id);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sanitize JSON input
     * Removes potentially dangerous JSON patterns
     */
    public String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // Remove script tags that might be in JSON strings
        json = XSS_SCRIPT_PATTERN.matcher(json).replaceAll("");

        // Validate JSON structure (basic check)
        int braceCount = 0;
        int bracketCount = 0;

        for (char c : json.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            else if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;

            if (braceCount < 0 || bracketCount < 0) {
                log.warn("Invalid JSON structure detected");
                return "{}";
            }
        }

        if (braceCount != 0 || bracketCount != 0) {
            log.warn("Unbalanced JSON braces/brackets");
            return "{}";
        }

        return json;
    }

    /**
     * Validate URL format
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            new java.net.URL(url);
            // Only allow http and https protocols
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sanitize search filter values
     */
    public String sanitizeFilterValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Remove SQL injection patterns
        value = SQL_INJECTION_PATTERN.matcher(value).replaceAll("");

        // Remove XSS patterns
        value = XSS_SCRIPT_PATTERN.matcher(value).replaceAll("");
        value = XSS_EVENT_PATTERN.matcher(value).replaceAll("");

        // Trim and limit length
        value = value.trim();
        if (value.length() > 255) {
            value = value.substring(0, 255);
        }

        return value;
    }

    /**
     * Validate and sanitize pagination parameters
     */
    public int sanitizePaginationValue(Integer value, int defaultValue, int maxValue) {
        if (value == null || value < 0) {
            return defaultValue;
        }
        return Math.min(value, maxValue);
    }
}
