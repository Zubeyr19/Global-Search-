package com.globalsearch.controller.auth;

import com.globalsearch.dto.request.LoginRequest;
import com.globalsearch.dto.response.LoginResponse;
import com.globalsearch.entity.AuditLog;
import com.globalsearch.entity.Company;
import com.globalsearch.entity.User;
import com.globalsearch.repository.CompanyRepository;
import com.globalsearch.security.JwtTokenProvider;
import com.globalsearch.service.AuditLogService;
import com.globalsearch.service.auth.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;
    private final com.globalsearch.service.LoginAttemptService loginAttemptService;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        // Check if account is locked
        if (loginAttemptService.isBlocked(loginRequest.getUsername())) {
            long minutesRemaining = loginAttemptService.getMinutesUntilUnlock(loginRequest.getUsername());
            log.warn("Login attempt for locked account: {}", loginRequest.getUsername());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Account locked");
            error.put("message", String.format("Account is temporarily locked due to too many failed login attempts. Try again in %d minutes.", minutesRemaining));
            error.put("minutesUntilUnlock", minutesRemaining);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Load user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userDetailsService.loadUserEntityByUsername(loginRequest.getUsername());

            // Record successful login
            loginAttemptService.loginSucceeded(loginRequest.getUsername());

            // Generate tokens
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("tenantId", user.getTenantId());
            claims.put("email", user.getEmail());

            String accessToken = jwtTokenProvider.generateToken(userDetails, claims);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            // Get company name if exists
            String companyName = null;
            if (user.getCompanyId() != null) {
                Company company = companyRepository.findById(user.getCompanyId()).orElse(null);
                if (company != null) {
                    companyName = company.getName();
                }
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());

            // Audit log - successful login
            auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN, user.getId(), user.getUsername(),
                    user.getTenantId(), request, 200, null);

            // Build response
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtExpiration / 1000) // Convert to seconds
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .tenantId(user.getTenantId())
                    .companyName(companyName)
                    .roles(user.getRoles().stream()
                            .map(Enum::name)
                            .collect(Collectors.toList()))
                    .build();

            log.info("User {} logged in successfully", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", loginRequest.getUsername());

            // Record failed login attempt
            loginAttemptService.loginFailed(loginRequest.getUsername());

            // Audit log - failed login
            try {
                User user = userDetailsService.loadUserEntityByUsername(loginRequest.getUsername());
                auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN_FAILED, user.getId(),
                        user.getUsername(), user.getTenantId(), request, 401, "Invalid credentials");
            } catch (Exception ignored) {}

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid credentials");

            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginRequest.getUsername());
            if (remainingAttempts > 0) {
                error.put("message", String.format("Username or password is incorrect. %d attempts remaining before account lockout.", remainingAttempts));
                error.put("remainingAttempts", remainingAttempts);
            } else {
                error.put("message", "Invalid credentials. Account has been locked due to too many failed attempts.");
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            log.error("Login error: ", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Authentication failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        try {
            if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
                refreshToken = refreshToken.substring(7);
            }

            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            String username = jwtTokenProvider.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = userDetailsService.loadUserEntityByUsername(username);

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("tenantId", user.getTenantId());
            claims.put("email", user.getEmail());

            String newAccessToken = jwtTokenProvider.generateToken(userDetails, claims);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpiration / 1000);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh error: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // Get user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                User user = userDetailsService.loadUserEntityByUsername(auth.getName());
                auditLogService.logAuthEvent(AuditLog.AuditAction.LOGOUT, user.getId(),
                        user.getUsername(), user.getTenantId(), request, 200, null);
            } catch (Exception ignored) {}
        }

        SecurityContextHolder.clearContext();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userDetailsService.loadUserEntityByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("tenantId", user.getTenantId());
            response.put("roles", user.getRoles());

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Not authenticated"));
    }
}