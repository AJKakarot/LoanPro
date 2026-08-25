package com.loanpro.modules.identity.dto;

import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.domain.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserStatus status,
        List<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.roleNames(),
                user.getCreatedAt()
        );
    }
}
