package com.loanpro.modules.customer.dto;

import com.loanpro.modules.customer.domain.EmploymentType;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Size(max = 20) String phone,
        @Past LocalDate dateOfBirth,
        @Size(max = 20) String gender,
        @Size(max = 40) String nationalId,
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
