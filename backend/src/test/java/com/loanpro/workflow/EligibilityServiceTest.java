package com.loanpro.workflow;

import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.workflow.service.EligibilityService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    @Test
    void calculatesEmiAndFlagsHighDtiAsIneligible() {
        LoanApplication application = new LoanApplication();
        application.setRequestedAmount(new BigDecimal("2000000"));
        application.setInterestRate(new BigDecimal("14.500"));
        application.setTenureMonths(12);
        application.setMonthlyIncome(new BigDecimal("20000"));
        application.setExistingEmis(new BigDecimal("15000"));
        application.setProcessingFeePercent(new BigDecimal("3.000"));

        var result = service.evaluate(application);

        assertTrue(result.estimatedEmi().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.debtToIncomeRatio().compareTo(new BigDecimal("0.50")) > 0);
        assertEquals(false, result.eligible());
    }

    @Test
    void marksHealthyProfileEligible() {
        LoanApplication application = new LoanApplication();
        application.setRequestedAmount(new BigDecimal("300000"));
        application.setInterestRate(new BigDecimal("11.250"));
        application.setTenureMonths(24);
        application.setMonthlyIncome(new BigDecimal("180000"));
        application.setExistingEmis(new BigDecimal("10000"));
        application.setProcessingFeePercent(new BigDecimal("1.500"));

        var result = service.evaluate(application);

        assertTrue(result.eligible());
        assertTrue(result.nv3Score() >= 600);
    }
}
