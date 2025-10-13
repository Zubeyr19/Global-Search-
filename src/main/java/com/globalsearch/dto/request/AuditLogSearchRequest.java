package com.globalsearch.dto.request;

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
public class AuditLogSearchRequest {
    private Long userId;
    private String username;
    private String tenantId;
    private AuditLog.AuditAction action;
    private String entityType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
