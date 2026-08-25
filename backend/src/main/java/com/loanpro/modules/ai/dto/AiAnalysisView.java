package com.loanpro.modules.ai.dto;

import java.util.List;

public record AiAnalysisView(
        boolean available,
        String message,
        String documentStatus,
        String eligibilityAssessment,
        String riskLevel,
        List<String> keyIssues,
        List<String> recommendedManualChecks,
        String summary,
        String disclaimer,
        AiAnalyzeResponse raw
) {
    public static AiAnalysisView unavailable(String message) {
        return new AiAnalysisView(
                false,
                message,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                "AI-generated analysis. Final decision must be made by an authorized human reviewer.",
                null
        );
    }
}
