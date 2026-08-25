package com.loanpro.modules.application.dto;

import com.loanpro.modules.application.domain.MakerReview;

import java.time.Instant;
import java.util.UUID;

public record MakerReviewResponse(
        UUID id,
        UUID makerId,
        String makerName,
        boolean customerInfoVerified,
        boolean documentsVerified,
        boolean financialsVerified,
        String remarks,
        String missingInformation,
        Instant createdAt
) {
    public static MakerReviewResponse from(MakerReview review) {
        return new MakerReviewResponse(
                review.getId(),
                review.getMaker().getId(),
                review.getMaker().getFullName(),
                review.isCustomerInfoVerified(),
                review.isDocumentsVerified(),
                review.isFinancialsVerified(),
                review.getRemarks(),
                review.getMissingInformation(),
                review.getCreatedAt()
        );
    }
}
