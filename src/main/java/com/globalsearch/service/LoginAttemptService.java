package com.globalsearch.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Service to track and manage login attempts
 * Implements account lockout after multiple failed attempts
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int ATTEMPT_CACHE_DURATION_MINUTES = 60;

    // Cache for tracking failed login attempts by username
    private final Cache<String, Integer> attemptsCache;

    // Cache for tracking lockout end time by username
    private final Cache<String, LocalDateTime> lockoutCache;

    public LoginAttemptService() {
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(ATTEMPT_CACHE_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.lockoutCache = Caffeine.newBuilder()
                .expireAfterWrite(LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    /**
     * Record a successful login attempt
     * Clears any previous failed attempts
     */
    public void loginSucceeded(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        attemptsCache.invalidate(username);
        lockoutCache.invalidate(username);
        log.debug("Login succeeded for user: {}, cleared failed attempts", username);
    }

    /**
     * Record a failed login attempt
     * Locks account after MAX_ATTEMPTS failures
     */
    public void loginFailed(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        int attempts = attemptsCache.get(username, key -> 0) + 1;
        attemptsCache.put(username, attempts);

        log.warn("Login failed for user: {}, attempt {} of {}", username, attempts, MAX_ATTEMPTS);

        if (attempts >= MAX_ATTEMPTS) {
            LocalDateTime lockoutEndTime = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
            lockoutCache.put(username, lockoutEndTime);
            log.warn("Account locked for user: {} until {}", username, lockoutEndTime);
        }
    }

    /**
     * Check if an account is locked
     */
    public boolean isBlocked(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        LocalDateTime lockoutEndTime = lockoutCache.getIfPresent(username);
        if (lockoutEndTime == null) {
            return false;
        }

        // Check if lockout period has expired
        if (LocalDateTime.now().isAfter(lockoutEndTime)) {
            lockoutCache.invalidate(username);
            attemptsCache.invalidate(username);
            log.info("Lockout period expired for user: {}", username);
            return false;
        }

        return true;
    }

    /**
     * Get the number of failed login attempts for a user
     */
    public int getFailedAttempts(String username) {
        if (username == null || username.isEmpty()) {
            return 0;
        }

        Integer attempts = attemptsCache.getIfPresent(username);
        return attempts != null ? attempts : 0;
    }

    /**
     * Get remaining attempts before lockout
     */
    public int getRemainingAttempts(String username) {
        int failedAttempts = getFailedAttempts(username);
        return Math.max(0, MAX_ATTEMPTS - failedAttempts);
    }

    /**
     * Get the lockout end time for a user
     * Returns null if not locked out
     */
    public LocalDateTime getLockoutEndTime(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        return lockoutCache.getIfPresent(username);
    }

    /**
     * Get minutes remaining until account unlock
     * Returns 0 if not locked
     */
    public long getMinutesUntilUnlock(String username) {
        LocalDateTime lockoutEndTime = getLockoutEndTime(username);
        if (lockoutEndTime == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(lockoutEndTime)) {
            return 0;
        }

        return java.time.Duration.between(now, lockoutEndTime).toMinutes();
    }

    /**
     * Manually unlock an account (admin function)
     */
    public void unlockAccount(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        attemptsCache.invalidate(username);
        lockoutCache.invalidate(username);
        log.info("Account manually unlocked for user: {}", username);
    }

    /**
     * Reset failed attempts for a user
     */
    public void resetFailedAttempts(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }

        attemptsCache.invalidate(username);
        log.debug("Failed attempts reset for user: {}", username);
    }

    /**
     * Check if user is close to being locked out
     */
    public boolean isNearLockout(String username) {
        int failedAttempts = getFailedAttempts(username);
        return failedAttempts >= (MAX_ATTEMPTS - 2); // Within 2 attempts of lockout
    }

    /**
     * Get lockout configuration
     */
    public LockoutConfiguration getConfiguration() {
        return new LockoutConfiguration(MAX_ATTEMPTS, LOCKOUT_DURATION_MINUTES);
    }

    /**
     * Get lockout status for a user
     */
    public LockoutStatus getLockoutStatus(String username) {
        boolean isBlocked = isBlocked(username);
        int failedAttempts = getFailedAttempts(username);
        int remainingAttempts = getRemainingAttempts(username);
        LocalDateTime lockoutEndTime = getLockoutEndTime(username);
        long minutesUntilUnlock = getMinutesUntilUnlock(username);

        return new LockoutStatus(
            username,
            isBlocked,
            failedAttempts,
            remainingAttempts,
            lockoutEndTime,
            minutesUntilUnlock
        );
    }

    /**
     * Lockout configuration holder
     */
    public static class LockoutConfiguration {
        private final int maxAttempts;
        private final int lockoutDurationMinutes;

        public LockoutConfiguration(int maxAttempts, int lockoutDurationMinutes) {
            this.maxAttempts = maxAttempts;
            this.lockoutDurationMinutes = lockoutDurationMinutes;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public int getLockoutDurationMinutes() {
            return lockoutDurationMinutes;
        }
    }

    /**
     * Lockout status holder
     */
    public static class LockoutStatus {
        private final String username;
        private final boolean isLocked;
        private final int failedAttempts;
        private final int remainingAttempts;
        private final LocalDateTime lockoutEndTime;
        private final long minutesUntilUnlock;

        public LockoutStatus(String username, boolean isLocked, int failedAttempts,
                           int remainingAttempts, LocalDateTime lockoutEndTime, long minutesUntilUnlock) {
            this.username = username;
            this.isLocked = isLocked;
            this.failedAttempts = failedAttempts;
            this.remainingAttempts = remainingAttempts;
            this.lockoutEndTime = lockoutEndTime;
            this.minutesUntilUnlock = minutesUntilUnlock;
        }

        public String getUsername() {
            return username;
        }

        public boolean isLocked() {
            return isLocked;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public int getRemainingAttempts() {
            return remainingAttempts;
        }

        public LocalDateTime getLockoutEndTime() {
            return lockoutEndTime;
        }

        public long getMinutesUntilUnlock() {
            return minutesUntilUnlock;
        }
    }
}
