package com.globalsearch.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void testUserBuilder_Success() {
        // Arrange & Act
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .tenantId("TENANT_A")
                .companyId(1L)
                .roles(Set.of(User.Role.MANAGER))
                .enabled(true)
                .build();

        // Assert
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getTenantId()).isEqualTo("TENANT_A");
        assertThat(user.getRoles()).contains(User.Role.MANAGER);
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    void testGetFullName_BothNamesPresent() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("John Doe");
    }

    @Test
    void testGetFullName_OnlyUsername() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .build();

        // Act
        String fullName = user.getFullName();

        // Assert
        assertThat(fullName).isEqualTo("testuser");
    }

    @Test
    void testHasRole_Success() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .roles(Set.of(User.Role.MANAGER, User.Role.VIEWER))
                .build();

        // Act & Assert
        assertThat(user.hasRole(User.Role.MANAGER)).isTrue();
        assertThat(user.hasRole(User.Role.VIEWER)).isTrue();
        assertThat(user.hasRole(User.Role.SUPER_ADMIN)).isFalse();
    }

    @Test
    void testIsAdmin_SuperAdmin() {
        // Arrange
        User user = User.builder()
                .username("admin")
                .roles(Set.of(User.Role.SUPER_ADMIN))
                .build();

        // Act & Assert
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void testIsAdmin_TenantAdmin() {
        // Arrange
        User user = User.builder()
                .username("tenantadmin")
                .roles(Set.of(User.Role.TENANT_ADMIN))
                .build();

        // Act & Assert
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void testIsAdmin_RegularUser() {
        // Arrange
        User user = User.builder()
                .username("regularuser")
                .roles(Set.of(User.Role.VIEWER))
                .build();

        // Act & Assert
        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void testPrePersist_SetsTimestamps() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .tenantId("TENANT_A")
                .build();

        // Act
        user.onCreate();

        // Assert
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
    }

    @Test
    void testPreUpdate_UpdatesTimestamp() throws Exception {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .tenantId("TENANT_A")
                .build();

        user.onCreate();
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();

        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);

        // Act
        user.onUpdate();

        // Assert
        assertThat(user.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(user.getCreatedAt()).isEqualTo(user.getCreatedAt()); // Created at shouldn't change
    }

    @Test
    void testUserRoles_AllRolesExist() {
        // Assert
        assertThat(User.Role.SUPER_ADMIN).isNotNull();
        assertThat(User.Role.TENANT_ADMIN).isNotNull();
        assertThat(User.Role.MANAGER).isNotNull();
        assertThat(User.Role.OPERATOR).isNotNull();
        assertThat(User.Role.VIEWER).isNotNull();
    }
}
