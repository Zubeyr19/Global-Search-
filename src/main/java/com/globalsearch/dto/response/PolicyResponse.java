package com.globalsearch.dto.response;

import com.globalsearch.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PolicyResponse {
    private Long id;
    private String name;
    private String description;
    private String tenantId;
    private User.Role role;
    private String rules;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
