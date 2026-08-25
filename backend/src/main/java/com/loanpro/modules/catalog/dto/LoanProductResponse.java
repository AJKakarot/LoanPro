package com.loanpro.modules.catalog.dto;

import com.loanpro.modules.catalog.domain.LoanProduct;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanProductResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minTenureMonths,
        Integer maxTenureMonths,
        BigDecimal interestRate,
        BigDecimal processingFeePercent,
        String requiredDocuments,
        boolean active
) {
    public static LoanProductResponse from(LoanProduct product) {
        return new LoanProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getInterestRate(),
                product.getProcessingFeePercent(),
                product.getRequiredDocuments(),
                product.isActive()
        );
    }
}
