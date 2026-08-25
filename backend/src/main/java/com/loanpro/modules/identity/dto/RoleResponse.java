package com.loanpro.modules.identity.dto;

import com.loanpro.modules.identity.domain.Role;
import com.loanpro.modules.identity.domain.RoleName;

import java.util.UUID;

public record RoleResponse(UUID id, RoleName name, String description) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }
}
