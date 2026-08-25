package com.loanpro.modules.application.dto;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationSummaryResponse(
        UUID id,
        String applicationNumber,
        UUID customerId,
        String customerName,
        String customerEmail,
        UUID loanProductId,
        String loanProductName,
        BigDecimal requestedAmount,
        Integer tenureMonths,
        BigDecimal interestRate,
        String purpose,
        ApplicationStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationSummaryResponse from(LoanApplication application) {
        return new ApplicationSummaryResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getCustomer().getId(),
                application.getCustomer().getFullName(),
                application.getCustomer().getEmail(),
                application.getLoanProduct().getId(),
                application.getLoanProduct().getName(),
                application.getRequestedAmount(),
                application.getTenureMonths(),
                application.getInterestRate(),
                application.getPurpose(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
