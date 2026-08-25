package com.loanpro.modules.application.dto;

import com.loanpro.modules.customer.domain.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpsertApplicationRequest(
        @NotNull UUID loanProductId,
        @NotNull @DecimalMin("1000.00") BigDecimal requestedAmount,
        @NotNull @Min(1) Integer tenureMonths,
        @NotBlank @Size(max = 500) String purpose,
        @Size(max = 160) String fullName,
        @Past LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 40) String nationalId,
        @Size(max = 20) String phone,
        @Size(max = 180) String email,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city,
        @Size(max = 80) String state,
        @Size(max = 20) String postalCode,
        EmploymentType employmentType,
        @Size(max = 160) String employerName,
        @Size(max = 120) String designation,
        @PositiveOrZero Integer yearsEmployed,
        @PositiveOrZero BigDecimal monthlyIncome,
        @PositiveOrZero BigDecimal otherIncome,
        @PositiveOrZero BigDecimal existingEmis,
        @PositiveOrZero BigDecimal monthlyExpenses
) {
}
