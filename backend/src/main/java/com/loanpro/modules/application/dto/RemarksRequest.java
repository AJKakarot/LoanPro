package com.loanpro.modules.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RemarksRequest(
        @NotBlank @Size(max = 2000) String remarks,
        String missingInformation
) {
}
