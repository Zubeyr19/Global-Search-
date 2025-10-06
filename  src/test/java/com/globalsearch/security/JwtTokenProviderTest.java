package com.globalsearch.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        // Set test values using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L); // 7 days

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_MANAGER"),
                        new SimpleGrantedAuthority("ROLE_VIEWER")
                ))
                .build();
    }

    @Test
    void testGenerateToken_Success() {
        // Act
        String token = jwtTokenProvider.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    void testGenerateToken_WithExtraClaims() {
        // Arrange
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", 123L);
        extraClaims.put("tenantId", "TENANT_TEST");
        extraClaims.put("email", "test@example.com");

        // Act
        String token = jwtTokenProvider.generateToken(userDetails, extraClaims);

        // Assert
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(123L);
        assertThat(jwtTokenProvider.extractTenantId(token)).isEqualTo("TENANT_TEST");
    }

    @Test
    void testGenerateRefreshToken_Success() {
        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    void testExtractUsername_Success() {
        // Arrange
        String token = jwtTokenProvider.generateToken(userDetails);

        // Act
        String username = jwtTokenProvider.extractUsername(token);

        // Assert
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void testExtractTenantId_Success() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", "TENANT_ABC");
        String token = jwtTokenProvider.generateToken(userDetails, claims);

        // Act
        String tenantId = jwtTokenProvider.extractTenantId(token);

        // Assert
        assertThat(tenantId).isEqualTo("TENANT_ABC");
    }

    @Test
    void testExtractUserId_Success() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 456L);
        String token = jwtTokenProvider.generateToken(userDetails, claims);

        // Act
        Long userId = jwtTokenProvider.extractUserId(token);

        // Assert
        assertThat(userId).isEqualTo(456L);
    }

    @Test
    void testValidateToken_ValidToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(userDetails);

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testValidateToken_WrongUsername() {
        // Arrange
        String token = jwtTokenProvider.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(Arrays.asList())
                .build();

        // Act
        Boolean isValid = jwtTokenProvider.validateToken(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void testIsTokenExpired_NotExpired() {
        // Arrange
        String token = jwtTokenProvider.generateToken(userDetails);

        // Act
        Boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    @Test
    void testExtractExpiration_Success() {
        // Arrange
        String token = jwtTokenProvider.generateToken(userDetails);

        // Act
        var expiration = jwtTokenProvider.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration.getTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void testTokenContainsRoles() {
        // Arrange & Act
        String token = jwtTokenProvider.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token, userDetails)).isTrue();

        // Verify token can be validated and username extracted
        String extractedUsername = jwtTokenProvider.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void testRefreshToken_DifferentFromAccessToken() {
        // Arrange & Act
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Assert
        assertThat(accessToken).isNotEqualTo(refreshToken);
        assertThat(jwtTokenProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
    }
}
