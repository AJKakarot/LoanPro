package com.loanpro.modules.application.dto;

import jakarta.validation.constraints.NotNull;

public record MakerVerifyRequest(
        @NotNull Boolean customerInfoVerified,
        @NotNull Boolean documentsVerified,
        @NotNull Boolean financialsVerified,
        String remarks
) {
}
