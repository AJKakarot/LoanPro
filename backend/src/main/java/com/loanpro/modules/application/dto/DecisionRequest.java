package com.loanpro.modules.application.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @Size(max = 2000) String remarks,
        @Size(max = 2000) String reason
) {
    public DecisionRequest {
        if (remarks == null) {
            remarks = "";
        }
    }
}
