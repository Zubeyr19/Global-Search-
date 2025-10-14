package com.globalsearch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoResponse {
    private String tenantId;
    private String tenantName;
    private Long userCount;
    private Long companyCount;
    private Long locationCount;
    private Long sensorCount;
    private Long searchCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private Long storageUsedMB;
}
