package com.globalsearch.dto.request;

import com.globalsearch.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {
    private Set<User.Role> roles;
}
