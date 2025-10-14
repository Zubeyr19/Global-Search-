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
public class RecentActivityDTO {
    private Long userId;
    private String username;
    private String tenantId;
    private String action;
    private String entityType;
    private String entityName;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String requestMethod;
    private Integer responseStatus;
}
