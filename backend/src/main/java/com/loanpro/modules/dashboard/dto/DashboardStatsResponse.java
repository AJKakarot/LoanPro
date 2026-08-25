package com.loanpro.modules.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
        long totalApplications,
        long pendingApplications,
        long makerReview,
        long checkerReview,
        long approved,
        long rejected,
        BigDecimal totalLoanAmount,
        BigDecimal approvedLoanAmount,
        List<com.loanpro.modules.application.dto.ApplicationSummaryResponse> recentApplications
) {
}
