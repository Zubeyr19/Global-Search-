package com.globalsearch.dto.response;

import com.globalsearch.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String tenantId;
    private AuditLog.AuditAction action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestUrl;
    private Integer responseStatus;
    private Long responseTimeMs;
    private String errorMessage;

    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .tenantId(auditLog.getTenantId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .entityName(auditLog.getEntityName())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .timestamp(auditLog.getTimestamp())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .requestMethod(auditLog.getRequestMethod())
                .requestUrl(auditLog.getRequestUrl())
                .responseStatus(auditLog.getResponseStatus())
                .responseTimeMs(auditLog.getResponseTimeMs())
                .errorMessage(auditLog.getErrorMessage())
                .build();
    }
}
