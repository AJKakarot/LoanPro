package com.loanpro.modules.identity.dto;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 20) String phone
) {
}
