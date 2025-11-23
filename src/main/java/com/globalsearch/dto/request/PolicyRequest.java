package com.globalsearch.dto.request;

import com.globalsearch.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PolicyRequest {
    @NotBlank(message = "Policy name is required")
    private String name;

    private String description;

    @NotNull(message = "Role is required")
    private User.Role role;

    @NotBlank(message = "Rules are required")
    private String rules;

    private Boolean active = true;
}

