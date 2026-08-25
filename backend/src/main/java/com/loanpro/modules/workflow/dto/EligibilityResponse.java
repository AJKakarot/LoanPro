package com.loanpro.modules.workflow.dto;

import java.math.BigDecimal;

public record EligibilityResponse(
        BigDecimal estimatedEmi,
        BigDecimal debtToIncomeRatio,
        int nv3Score,
        String riskBand,
        boolean eligible,
        String summary,
        BigDecimal processingFee,
        BigDecimal feesPercent,
        BigDecimal interestRate
) {
}
