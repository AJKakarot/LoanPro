package com.loanpro.modules.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LoanProductRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        String description,
        @NotNull @DecimalMin("1.00") BigDecimal minAmount,
        @NotNull @DecimalMin("1.00") BigDecimal maxAmount,
        @NotNull @Min(1) Integer minTenureMonths,
        @NotNull @Min(1) Integer maxTenureMonths,
        @NotNull @DecimalMin("0.010") BigDecimal interestRate,
        @NotNull @DecimalMin("0.000") BigDecimal processingFeePercent,
        @NotBlank String requiredDocuments,
        Boolean active
) {
}
