package com.loanpro.modules.workflow.service;

import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.workflow.dto.EligibilityResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class EligibilityService {

    private static final BigDecimal MAX_DTI = new BigDecimal("0.50");
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    public EligibilityResponse evaluate(LoanApplication application) {
        BigDecimal principal = nvl(application.getRequestedAmount());
        BigDecimal annualRate = nvl(application.getInterestRate());
        int tenure = application.getTenureMonths() == null ? 1 : application.getTenureMonths();
        BigDecimal monthlyIncome = nvl(application.getMonthlyIncome());
        BigDecimal existingEmis = nvl(application.getExistingEmis());
        BigDecimal feePercent = nvl(application.getProcessingFeePercent());

        BigDecimal emi = calculateEmi(principal, annualRate, tenure);
        BigDecimal dti = BigDecimal.ZERO;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            dti = existingEmis.add(emi).divide(monthlyIncome, 4, RoundingMode.HALF_UP);
        }
        BigDecimal processingFee = principal.multiply(feePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        int score = nv3Score(monthlyIncome, dti, principal, annualRate);
        String riskBand = score >= 750 ? "LOW" : score >= 600 ? "MEDIUM" : "HIGH";
        boolean eligible = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
                && dti.compareTo(MAX_DTI) <= 0
                && score >= 550;
        String summary = eligible
                ? "Applicant meets policy thresholds for debt-to-income and income coverage."
                : "Applicant is outside policy thresholds. Review DTI, income stability and requested amount.";

        return new EligibilityResponse(
                emi,
                dti,
                score,
                riskBand,
                eligible,
                summary,
                processingFee,
                feePercent,
                annualRate
        );
    }

    BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualPercent, int months) {
        if (months <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualPercent.divide(new BigDecimal("1200"), MC);
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal pow = onePlusR.pow(months, MC);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(pow);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private int nv3Score(BigDecimal income, BigDecimal dti, BigDecimal amount, BigDecimal rate) {
        int incomeScore = income.compareTo(new BigDecimal("100000")) >= 0 ? 300
                : income.compareTo(new BigDecimal("50000")) >= 0 ? 220
                : income.compareTo(new BigDecimal("25000")) >= 0 ? 140 : 60;
        int dtiScore = dti.compareTo(new BigDecimal("0.30")) <= 0 ? 300
                : dti.compareTo(new BigDecimal("0.40")) <= 0 ? 220
                : dti.compareTo(new BigDecimal("0.50")) <= 0 ? 140 : 40;
        int amountScore = amount.compareTo(new BigDecimal("500000")) <= 0 ? 200 : 120;
        int rateScore = rate.compareTo(new BigDecimal("12")) <= 0 ? 200 : 140;
        return Math.min(990, incomeScore + dtiScore + amountScore + rateScore);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
