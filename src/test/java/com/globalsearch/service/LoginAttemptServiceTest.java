package com.globalsearch.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoginAttemptService
 */
class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    @DisplayName("Should not block account initially")
    void testInitialState() {
        String username = "testuser";

        assertFalse(loginAttemptService.isBlocked(username));
        assertEquals(0, loginAttemptService.getFailedAttempts(username));
        assertEquals(5, loginAttemptService.getRemainingAttempts(username));
    }

    @Test
    @DisplayName("Should track failed login attempts")
    void testTrackFailedAttempts() {
        String username = "testuser";

        loginAttemptService.loginFailed(username);
        assertEquals(1, loginAttemptService.getFailedAttempts(username));
        assertEquals(4, loginAttemptService.getRemainingAttempts(username));

        loginAttemptService.loginFailed(username);
        assertEquals(2, loginAttemptService.getFailedAttempts(username));
        assertEquals(3, loginAttemptService.getRemainingAttempts(username));
    }

    @Test
    @DisplayName("Should lock account after 5 failed attempts")
    void testAccountLockoutAfterMaxAttempts() {
        String username = "testuser";

        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(username);
        }

        assertTrue(loginAttemptService.isBlocked(username));
        assertNotNull(loginAttemptService.getLockoutEndTime(username));
        assertTrue(loginAttemptService.getMinutesUntilUnlock(username) > 0);
    }

    @Test
    @DisplayName("Should clear attempts on successful login")
    void testClearAttemptsOnSuccess() {
        String username = "testuser";

        // Fail a few times
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        assertEquals(3, loginAttemptService.getFailedAttempts(username));

        // Successful login
        loginAttemptService.loginSucceeded(username);

        assertEquals(0, loginAttemptService.getFailedAttempts(username));
        assertFalse(loginAttemptService.isBlocked(username));
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void testNullUsername() {
        assertFalse(loginAttemptService.isBlocked(null));
        assertEquals(0, loginAttemptService.getFailedAttempts(null));
        assertEquals(5, loginAttemptService.getRemainingAttempts(null));

        // Should not throw exception
        assertDoesNotThrow(() -> loginAttemptService.loginFailed(null));
        assertDoesNotThrow(() -> loginAttemptService.loginSucceeded(null));
    }

    @Test
    @DisplayName("Should manually unlock account")
    void testManualUnlock() {
        String username = "testuser";

        // Lock the account
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(username);
        }

        assertTrue(loginAttemptService.isBlocked(username));

        // Manually unlock
        loginAttemptService.unlockAccount(username);

        assertFalse(loginAttemptService.isBlocked(username));
        assertEquals(0, loginAttemptService.getFailedAttempts(username));
    }

    @Test
    @DisplayName("Should reset failed attempts")
    void testResetFailedAttempts() {
        String username = "testuser";

        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        assertEquals(3, loginAttemptService.getFailedAttempts(username));

        loginAttemptService.resetFailedAttempts(username);

        assertEquals(0, loginAttemptService.getFailedAttempts(username));
    }

    @Test
    @DisplayName("Should detect when user is near lockout")
    void testNearLockout() {
        String username = "testuser";

        assertFalse(loginAttemptService.isNearLockout(username));

        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);
        assertFalse(loginAttemptService.isNearLockout(username));

        loginAttemptService.loginFailed(username);
        assertTrue(loginAttemptService.isNearLockout(username));

        loginAttemptService.loginFailed(username);
        assertTrue(loginAttemptService.isNearLockout(username));
    }

    @Test
    @DisplayName("Should get lockout configuration")
    void testGetConfiguration() {
        LoginAttemptService.LockoutConfiguration config = loginAttemptService.getConfiguration();

        assertNotNull(config);
        assertEquals(5, config.getMaxAttempts());
        assertEquals(30, config.getLockoutDurationMinutes());
    }

    @Test
    @DisplayName("Should get complete lockout status")
    void testGetLockoutStatus() {
        String username = "testuser";

        loginAttemptService.loginFailed(username);
        loginAttemptService.loginFailed(username);

        LoginAttemptService.LockoutStatus status = loginAttemptService.getLockoutStatus(username);

        assertNotNull(status);
        assertEquals(username, status.getUsername());
        assertFalse(status.isLocked());
        assertEquals(2, status.getFailedAttempts());
        assertEquals(3, status.getRemainingAttempts());
        assertNull(status.getLockoutEndTime());
        assertEquals(0, status.getMinutesUntilUnlock());
    }

    @Test
    @DisplayName("Should handle multiple users independently")
    void testMultipleUsersIndependently() {
        String user1 = "user1";
        String user2 = "user2";

        loginAttemptService.loginFailed(user1);
        loginAttemptService.loginFailed(user1);
        loginAttemptService.loginFailed(user2);

        assertEquals(2, loginAttemptService.getFailedAttempts(user1));
        assertEquals(1, loginAttemptService.getFailedAttempts(user2));

        loginAttemptService.loginSucceeded(user1);

        assertEquals(0, loginAttemptService.getFailedAttempts(user1));
        assertEquals(1, loginAttemptService.getFailedAttempts(user2));
    }

    @Test
    @DisplayName("Should not block with empty username")
    void testEmptyUsername() {
        String username = "";

        assertFalse(loginAttemptService.isBlocked(username));
        assertDoesNotThrow(() -> loginAttemptService.loginFailed(username));
        assertDoesNotThrow(() -> loginAttemptService.loginSucceeded(username));
    }
}
