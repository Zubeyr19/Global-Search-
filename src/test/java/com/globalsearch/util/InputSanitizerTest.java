package com.globalsearch.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InputSanitizer
 */
class InputSanitizerTest {

    private InputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new InputSanitizer();
    }

    @Test
    @DisplayName("Should sanitize basic text by removing HTML tags")
    void testSanitizeBasicText() {
        String input = "<script>alert('xss')</script>Hello World";
        String sanitized = sanitizer.sanitizeText(input);

        assertFalse(sanitized.contains("<script>"));
        assertFalse(sanitized.contains("</script>"));
        assertTrue(sanitized.contains("Hello World"));
    }

    @Test
    @DisplayName("Should detect XSS attempts")
    void testDetectXSS() {
        assertTrue(sanitizer.containsXSS("<script>alert('xss')</script>"));
        assertTrue(sanitizer.containsXSS("<img src=x onerror=alert(1)>"));
        assertTrue(sanitizer.containsXSS("<iframe src='evil.com'></iframe>"));
        assertFalse(sanitizer.containsXSS("Normal text"));
    }

    @Test
    @DisplayName("Should detect SQL injection attempts")
    void testDetectSQLInjection() {
        assertTrue(sanitizer.containsSQLInjection("SELECT * FROM users"));
        assertTrue(sanitizer.containsSQLInjection("1' OR '1'='1"));
        assertTrue(sanitizer.containsSQLInjection("admin'--"));
        assertTrue(sanitizer.containsSQLInjection("DROP TABLE users"));
        assertFalse(sanitizer.containsSQLInjection("normal search query"));
    }

    @Test
    @DisplayName("Should detect path traversal attempts")
    void testDetectPathTraversal() {
        assertTrue(sanitizer.containsPathTraversal("../../../etc/passwd"));
        assertTrue(sanitizer.containsPathTraversal("..\\..\\windows\\system32"));
        assertTrue(sanitizer.containsPathTraversal("%2e%2e%2f"));
        assertFalse(sanitizer.containsPathTraversal("normal/file/path"));
    }

    @Test
    @DisplayName("Should validate email addresses")
    void testValidateEmail() {
        assertTrue(sanitizer.isValidEmail("user@example.com"));
        assertTrue(sanitizer.isValidEmail("test.user@example.co.uk"));
        assertTrue(sanitizer.isValidEmail("user+tag@example.com"));

        assertFalse(sanitizer.isValidEmail("invalid.email"));
        assertFalse(sanitizer.isValidEmail("@example.com"));
        assertFalse(sanitizer.isValidEmail("user@"));
        assertFalse(sanitizer.isValidEmail(null));
        assertFalse(sanitizer.isValidEmail(""));
    }

    @Test
    @DisplayName("Should validate usernames")
    void testValidateUsername() {
        assertTrue(sanitizer.isValidUsername("john_doe"));
        assertTrue(sanitizer.isValidUsername("user.name"));
        assertTrue(sanitizer.isValidUsername("user-123"));
        assertTrue(sanitizer.isValidUsername("abc"));

        assertFalse(sanitizer.isValidUsername("ab")); // Too short
        assertFalse(sanitizer.isValidUsername("user@name")); // Invalid char
        assertFalse(sanitizer.isValidUsername("user name")); // Space
        assertFalse(sanitizer.isValidUsername(null));
        assertFalse(sanitizer.isValidUsername(""));
    }

    @Test
    @DisplayName("Should validate filenames")
    void testValidateFilename() {
        assertTrue(sanitizer.isValidFilename("document.pdf"));
        assertTrue(sanitizer.isValidFilename("report_2024.xlsx"));
        assertTrue(sanitizer.isValidFilename("file-name.txt"));

        assertFalse(sanitizer.isValidFilename("../etc/passwd"));
        assertFalse(sanitizer.isValidFilename("file with spaces.txt"));
        assertFalse(sanitizer.isValidFilename("file@name.txt"));
        assertFalse(sanitizer.isValidFilename(null));
        assertFalse(sanitizer.isValidFilename(""));
    }

    @Test
    @DisplayName("Should sanitize filenames")
    void testSanitizeFilename() {
        assertEquals("document.pdf", sanitizer.sanitizeFilename("document.pdf"));
        assertEquals("file_name.txt", sanitizer.sanitizeFilename("file@name.txt"));
        assertEquals("file_name.txt", sanitizer.sanitizeFilename("file name.txt"));
        assertEquals("etc_passwd", sanitizer.sanitizeFilename("../etc/passwd"));
    }

    @Test
    @DisplayName("Should escape HTML special characters")
    void testEscapeHtml() {
        String input = "<script>alert('test')</script>";
        String escaped = sanitizer.escapeHtml(input);

        assertTrue(escaped.contains("&lt;"));
        assertTrue(escaped.contains("&gt;"));
        assertFalse(escaped.contains("<script>"));
    }

    @Test
    @DisplayName("Should validate tenant IDs")
    void testValidateTenantId() {
        assertTrue(sanitizer.isValidTenantId("TENANT_A"));
        assertTrue(sanitizer.isValidTenantId("COMPANY_123"));
        assertTrue(sanitizer.isValidTenantId("ORG"));

        assertFalse(sanitizer.isValidTenantId("tenant-a")); // Lowercase
        assertFalse(sanitizer.isValidTenantId("tenant a")); // Space
        assertFalse(sanitizer.isValidTenantId("tenant@123")); // Special char
        assertFalse(sanitizer.isValidTenantId("T")); // Too short
        assertFalse(sanitizer.isValidTenantId(null));
    }

    @Test
    @DisplayName("Should validate numeric IDs")
    void testValidateNumericId() {
        assertTrue(sanitizer.isValidNumericId("123"));
        assertTrue(sanitizer.isValidNumericId("1"));
        assertTrue(sanitizer.isValidNumericId("999999"));

        assertFalse(sanitizer.isValidNumericId("0"));
        assertFalse(sanitizer.isValidNumericId("-1"));
        assertFalse(sanitizer.isValidNumericId("abc"));
        assertFalse(sanitizer.isValidNumericId("12.34"));
        assertFalse(sanitizer.isValidNumericId(null));
        assertFalse(sanitizer.isValidNumericId(""));
    }

    @Test
    @DisplayName("Should validate URLs")
    void testValidateUrl() {
        assertTrue(sanitizer.isValidUrl("http://example.com"));
        assertTrue(sanitizer.isValidUrl("https://example.com/path"));
        assertTrue(sanitizer.isValidUrl("https://example.com:8080/api"));

        assertFalse(sanitizer.isValidUrl("ftp://example.com")); // Wrong protocol
        assertFalse(sanitizer.isValidUrl("javascript:alert(1)")); // Dangerous protocol
        assertFalse(sanitizer.isValidUrl("not a url"));
        assertFalse(sanitizer.isValidUrl(null));
        assertFalse(sanitizer.isValidUrl(""));
    }

    @Test
    @DisplayName("Should sanitize search queries")
    void testSanitizeSearchQuery() {
        String query = "sensor <script>alert(1)</script>";
        String sanitized = sanitizer.sanitizeSearchQuery(query);

        assertFalse(sanitized.contains("<script>"));
        assertTrue(sanitized.contains("sensor"));
    }

    @Test
    @DisplayName("Should sanitize filter values")
    void testSanitizeFilterValue() {
        String filter = "value'; DROP TABLE users--";
        String sanitized = sanitizer.sanitizeFilterValue(filter);

        assertFalse(sanitized.contains("DROP"));
        assertFalse(sanitized.contains("--"));
    }

    @Test
    @DisplayName("Should detect command injection")
    void testDetectCommandInjection() {
        assertTrue(sanitizer.containsCommandInjection("ls; rm -rf /"));
        assertTrue(sanitizer.containsCommandInjection("file.txt && cat /etc/passwd"));
        assertTrue(sanitizer.containsCommandInjection("`whoami`"));

        assertFalse(sanitizer.containsCommandInjection("normal text"));
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputs() {
        assertNull(sanitizer.sanitizeText(null));
        assertNull(sanitizer.sanitizeSearchQuery(null));
        assertNull(sanitizer.sanitizeFilterValue(null));
        assertNull(sanitizer.sanitizeFilename(null));
        assertNull(sanitizer.sanitizeJson(null));
        assertNull(sanitizer.escapeHtml(null));

        assertFalse(sanitizer.containsXSS(null));
        assertFalse(sanitizer.containsSQLInjection(null));
        assertFalse(sanitizer.containsPathTraversal(null));
        assertFalse(sanitizer.containsCommandInjection(null));
    }

    @Test
    @DisplayName("Should handle empty inputs gracefully")
    void testEmptyInputs() {
        assertEquals("", sanitizer.sanitizeText(""));
        assertEquals("", sanitizer.sanitizeSearchQuery(""));
        assertEquals("", sanitizer.sanitizeFilterValue(""));

        assertFalse(sanitizer.containsXSS(""));
        assertFalse(sanitizer.containsSQLInjection(""));
        assertFalse(sanitizer.containsPathTraversal(""));
    }

    @Test
    @DisplayName("Should sanitize pagination values")
    void testSanitizePagination() {
        assertEquals(10, sanitizer.sanitizePaginationValue(10, 0, 100));
        assertEquals(100, sanitizer.sanitizePaginationValue(150, 0, 100)); // Max limit
        assertEquals(0, sanitizer.sanitizePaginationValue(-5, 0, 100)); // Negative
        assertEquals(0, sanitizer.sanitizePaginationValue(null, 0, 100)); // Null
    }

    @Test
    @DisplayName("Should sanitize JSON input")
    void testSanitizeJson() {
        String validJson = "{\"name\": \"value\"}";
        assertEquals(validJson, sanitizer.sanitizeJson(validJson));

        String jsonWithScript = "{\"name\": \"<script>alert(1)</script>\"}";
        String sanitized = sanitizer.sanitizeJson(jsonWithScript);
        assertFalse(sanitized.contains("<script>"));

        String invalidJson = "{{{";
        String sanitizedInvalid = sanitizer.sanitizeJson(invalidJson);
        assertEquals("{}", sanitizedInvalid);
    }

    @Test
    @DisplayName("Should limit filter value length")
    void testFilterValueLengthLimit() {
        String longValue = "a".repeat(300);
        String sanitized = sanitizer.sanitizeFilterValue(longValue);

        assertTrue(sanitized.length() <= 255);
    }

    @Test
    @DisplayName("Should trim whitespace from sanitized text")
    void testTrimWhitespace() {
        String input = "  hello world  ";
        String sanitized = sanitizer.sanitizeText(input);

        assertEquals("hello world", sanitized);
    }

    @Test
    @DisplayName("Should remove multiple types of HTML tags")
    void testMultipleHtmlTags() {
        String input = "<div><p>Text</p><span>More</span></div>";
        String sanitized = sanitizer.sanitizeText(input);

        assertFalse(sanitized.contains("<div>"));
        assertFalse(sanitized.contains("<p>"));
        assertFalse(sanitized.contains("<span>"));
        assertTrue(sanitized.contains("Text"));
        assertTrue(sanitized.contains("More"));
    }
}
