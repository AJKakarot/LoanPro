package com.loanpro.modules.application.dto;

import com.loanpro.modules.application.domain.CheckerDecision;
import com.loanpro.modules.application.domain.CheckerReview;

import java.time.Instant;
import java.util.UUID;

public record CheckerReviewResponse(
        UUID id,
        UUID checkerId,
        String checkerName,
        CheckerDecision decision,
        String reason,
        String remarks,
        Instant createdAt
) {
    public static CheckerReviewResponse from(CheckerReview review) {
        return new CheckerReviewResponse(
                review.getId(),
                review.getChecker().getId(),
                review.getChecker().getFullName(),
                review.getDecision(),
                review.getReason(),
                review.getRemarks(),
                review.getCreatedAt()
        );
    }
}
