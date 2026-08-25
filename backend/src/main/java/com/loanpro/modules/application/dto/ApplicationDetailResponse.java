package com.loanpro.modules.application.dto;

import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.customer.domain.EmploymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationDetailResponse(
        UUID id,
        String applicationNumber,
        UUID customerId,
        String customerName,
        String customerEmail,
        UUID loanProductId,
        String loanProductName,
        String loanProductCode,
        BigDecimal requestedAmount,
        Integer tenureMonths,
        BigDecimal interestRate,
        BigDecimal processingFeePercent,
        String purpose,
        ApplicationStatus status,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String nationalId,
        String phone,
        String email,
        String addressLine,
        String city,
        String state,
        String postalCode,
        EmploymentType employmentType,
        String employerName,
        String designation,
        Integer yearsEmployed,
        BigDecimal monthlyIncome,
        BigDecimal otherIncome,
        BigDecimal existingEmis,
        BigDecimal monthlyExpenses,
        UUID assignedMakerId,
        String assignedMakerName,
        UUID assignedCheckerId,
        String assignedCheckerName,
        Instant submittedAt,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApplicationDetailResponse from(LoanApplication application) {
        return new ApplicationDetailResponse(
                application.getId(),
                application.getApplicationNumber(),
                application.getCustomer().getId(),
                application.getCustomer().getFullName(),
                application.getCustomer().getEmail(),
                application.getLoanProduct().getId(),
                application.getLoanProduct().getName(),
                application.getLoanProduct().getCode(),
                application.getRequestedAmount(),
                application.getTenureMonths(),
                application.getInterestRate(),
                application.getProcessingFeePercent(),
                application.getPurpose(),
                application.getStatus(),
                application.getFullName(),
                application.getDateOfBirth(),
                application.getGender(),
                application.getNationalId(),
                application.getPhone(),
                application.getEmail(),
                application.getAddressLine(),
                application.getCity(),
                application.getState(),
                application.getPostalCode(),
                application.getEmploymentType(),
                application.getEmployerName(),
                application.getDesignation(),
                application.getYearsEmployed(),
                application.getMonthlyIncome(),
                application.getOtherIncome(),
                application.getExistingEmis(),
                application.getMonthlyExpenses(),
                application.getAssignedMaker() != null ? application.getAssignedMaker().getId() : null,
                application.getAssignedMaker() != null ? application.getAssignedMaker().getFullName() : null,
                application.getAssignedChecker() != null ? application.getAssignedChecker().getId() : null,
                application.getAssignedChecker() != null ? application.getAssignedChecker().getFullName() : null,
                application.getSubmittedAt(),
                application.getDecidedAt(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
