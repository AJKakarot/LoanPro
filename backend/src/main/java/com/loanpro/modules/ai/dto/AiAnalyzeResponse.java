package com.loanpro.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalyzeResponse(
        DocumentAnalysis documentAnalysis,
        EligibilityAnalysis eligibilityAnalysis,
        RiskAnalysis riskAnalysis,
        VerificationSummary verificationSummary
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentAnalysis(
            String status,
            List<String> missingDocuments,
            List<String> mismatches,
            List<String> warnings,
            String summary
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EligibilityAnalysis(
            String assessment,
            List<String> factors,
            List<String> warnings,
            String summary,
            Double estimatedEmi,
            Double debtToIncomeRatio,
            Double disposableIncome
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RiskAnalysis(
            String riskLevel,
            List<String> riskFactors,
            List<String> warnings,
            List<String> manualChecks
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerificationSummary(
            String documentStatus,
            String eligibilityAssessment,
            String riskLevel,
            List<String> keyIssues,
            List<String> recommendedManualChecks,
            String summary,
            String disclaimer
    ) {}
}
