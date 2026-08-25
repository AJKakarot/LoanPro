package com.loanpro.modules.identity.dto;

import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.UserStatus;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 20) String phone,
        UserStatus status,
        Set<RoleName> roles
) {
}
