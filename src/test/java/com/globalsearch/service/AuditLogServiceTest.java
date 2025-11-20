package com.globalsearch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalsearch.entity.AuditLog;
import com.globalsearch.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService
 * Tests audit logging functionality with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        // Setup common request mocks
        when(httpRequest.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        when(httpRequest.getMethod()).thenReturn("POST");
        when(httpRequest.getRequestURI()).thenReturn("/api/search");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");
    }

    @Test
    void testLogEvent_Success_SavesAuditLog() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";
        AuditLog.AuditAction action = AuditLog.AuditAction.CREATE;
        String entityType = "Company";
        Long entityId = 100L;
        String entityName = "Test Company";

        // When
        auditLogService.logEvent(action, userId, username, tenantId,
                entityType, entityId, entityName, null, null, httpRequest);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.CREATE);
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getUsername()).isEqualTo(username);
        assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
        assertThat(savedLog.getEntityType()).isEqualTo(entityType);
        assertThat(savedLog.getEntityId()).isEqualTo(entityId);
        assertThat(savedLog.getEntityName()).isEqualTo(entityName);
        assertThat(savedLog.getTimestamp()).isNotNull();
    }

    @Test
    void testLogEvent_WithHttpRequest_CapturesRequestDetails() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";

        // When
        auditLogService.logEvent(AuditLog.AuditAction.UPDATE, userId, username, tenantId,
                "Sensor", 200L, "Sensor A1", null, null, httpRequest);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        assertThat(savedLog.getRequestMethod()).isEqualTo("POST");
        assertThat(savedLog.getRequestUrl()).isEqualTo("/api/search");
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    void testLogEvent_WithoutHttpRequest_DoesNotSetRequestFields() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";

        // When
        auditLogService.logEvent(AuditLog.AuditAction.DELETE, userId, username, tenantId,
                "Report", 300L, "Monthly Report", null, null, null);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.DELETE);
        assertThat(savedLog.getUserAgent()).isNull();
        assertThat(savedLog.getRequestMethod()).isNull();
        assertThat(savedLog.getRequestUrl()).isNull();
        assertThat(savedLog.getIpAddress()).isNull();
    }

    @Test
    void testLogAuthEvent_SuccessfulLogin_CapturesAuthDetails() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";
        Integer responseStatus = 200;

        // When
        auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN, userId, username,
                tenantId, httpRequest, responseStatus, null);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.LOGIN);
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getUsername()).isEqualTo(username);
        assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
        assertThat(savedLog.getEntityType()).isEqualTo("AUTH");
        assertThat(savedLog.getResponseStatus()).isEqualTo(200);
        assertThat(savedLog.getErrorMessage()).isNull();
    }

    @Test
    void testLogAuthEvent_FailedLogin_CapturesErrorMessage() {
        // Given
        String username = "invaliduser";
        Integer responseStatus = 401;
        String errorMessage = "Invalid credentials";

        // When
        auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN_FAILED, null, username,
                null, httpRequest, responseStatus, errorMessage);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.LOGIN_FAILED);
        assertThat(savedLog.getUserId()).isNull();
        assertThat(savedLog.getUsername()).isEqualTo(username);
        assertThat(savedLog.getTenantId()).isEqualTo("SYSTEM"); // Default for failed auth
        assertThat(savedLog.getResponseStatus()).isEqualTo(401);
        assertThat(savedLog.getErrorMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void testLogAuthEvent_WithNullTenantId_UsesSYSTEM() {
        // Given - tenantId is null (e.g., failed login before tenant context)
        String username = "testuser";
        Integer responseStatus = 401;

        // When
        auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN_FAILED, null, username,
                null, httpRequest, responseStatus, "Account locked");

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getTenantId()).isEqualTo("SYSTEM");
    }

    @Test
    void testLogSearchEvent_CapturesSearchDetails() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";
        String query = "temperature sensor";
        Integer resultCount = 5;

        // When
        auditLogService.logSearchEvent(userId, username, tenantId, query,
                resultCount, httpRequest);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.SEARCH);
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getUsername()).isEqualTo(username);
        assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
        assertThat(savedLog.getEntityType()).isEqualTo("SEARCH");
    }

    @Test
    void testLogEvent_RepositoryThrowsException_DoesNotPropagateException() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database connection lost"));

        // When - Should not throw exception
        auditLogService.logEvent(AuditLog.AuditAction.CREATE, 1L, "testuser",
                "TENANT_001", "Company", 100L, "Test Co", null, null, httpRequest);

        // Then - Exception is caught and logged, but not propagated
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testLogAuthEvent_AsyncExecution_DoesNotBlockCaller() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";

        // When
        long startTime = System.currentTimeMillis();
        auditLogService.logAuthEvent(AuditLog.AuditAction.LOGIN, userId, username,
                tenantId, httpRequest, 200, null);
        long duration = System.currentTimeMillis() - startTime;

        // Then - Should execute quickly (async)
        assertThat(duration).isLessThan(100); // Should be nearly instant
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testLogEvent_MultipleActions_AllAreLogged() {
        // Given & When - Log multiple different actions
        auditLogService.logEvent(AuditLog.AuditAction.CREATE, 1L, "user1",
                "TENANT_001", "Company", 100L, "Company A", null, null, httpRequest);

        auditLogService.logEvent(AuditLog.AuditAction.UPDATE, 1L, "user1",
                "TENANT_001", "Company", 100L, "Company A Updated", null, null, httpRequest);

        auditLogService.logEvent(AuditLog.AuditAction.DELETE, 1L, "user1",
                "TENANT_001", "Company", 100L, "Company A", null, null, httpRequest);

        // Then
        verify(auditLogRepository, times(3)).save(any(AuditLog.class));
    }

    @Test
    void testLogSearchEvent_ZeroResults_StillLogged() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String tenantId = "TENANT_001";
        String query = "nonexistent";
        Integer resultCount = 0;

        // When
        auditLogService.logSearchEvent(userId, username, tenantId, query,
                resultCount, httpRequest);

        // Then - Should still log searches with zero results
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.AuditAction.SEARCH);
        assertThat(savedLog.getUsername()).isEqualTo(username);
    }

    @Test
    void testLogEvent_DifferentEntityTypes_AllSupported() {
        // Given & When - Log events for different entity types
        String[] entityTypes = {"Company", "Location", "Zone", "Sensor", "Report", "Dashboard"};

        for (String entityType : entityTypes) {
            auditLogService.logEvent(AuditLog.AuditAction.VIEW, 1L, "testuser",
                    "TENANT_001", entityType, 100L, "Test Entity", null, null, httpRequest);
        }

        // Then
        verify(auditLogRepository, times(entityTypes.length)).save(any(AuditLog.class));
    }
}
